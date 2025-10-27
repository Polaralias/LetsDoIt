package com.polaralias.letsdoit.ai.speech

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.nio.ByteBuffer
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

@Singleton
class LocalSpeechRecognizerEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : SttEngine {
    private val languageCache: List<Locale> by lazy { loadLanguages() }

    override fun supported(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    override fun languages(): List<Locale> {
        return languageCache
    }

    override suspend fun transcribeLive(
        mic: AudioSource,
        lang: Locale,
        offlinePreferred: Boolean,
        diarise: Boolean,
        onPartial: (TranscriptChunk) -> Unit
    ): TranscriptResult {
        return recognise(intent = buildBaseIntent(lang, offlinePreferred), lang.toLanguageTag()) { bundle, start, assembler ->
            handlePartial(bundle, start, assembler, onPartial)
        }
    }

    override suspend fun transcribeFile(
        uri: android.net.Uri,
        lang: Locale,
        diarise: Boolean,
        onPartial: (TranscriptChunk) -> Unit
    ): TranscriptResult {
        val format = resolveFormat(uri)
        val pipe = ParcelFileDescriptor.createPipe()
        return coroutineScope {
            val writer = launch(Dispatchers.IO) {
                ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]).use { output ->
                    decodeToPipe(uri, format, output)
                }
            }
            try {
                recognise(
                    intent = buildFileIntent(lang, pipe[0], format),
                    lang.toLanguageTag(),
                    onPartial = { bundle, start, assembler ->
                        handlePartial(bundle, start, assembler, onPartial)
                    }
                )
            } finally {
                writer.cancelAndJoin()
                pipe[0].close()
            }
        }
    }

    private suspend fun recognise(
        intent: Intent,
        langTag: String,
        onPartial: (Bundle, Long, PartialChunkAssembler) -> Unit
    ): TranscriptResult {
        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                val assembler = PartialChunkAssembler()
                val startTime = SystemClock.elapsedRealtime()
                var completed = false
                val listener = object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}

                    override fun onBeginningOfSpeech() {}

                    override fun onRmsChanged(rmsdB: Float) {}

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {}

                    override fun onError(error: Int) {
                        if (!completed) {
                            completed = true
                            recognizer.cancel()
                            recognizer.destroy()
                            continuation.resumeWithException(IOException("Speech recogniser error: $error"))
                        }
                    }

                    override fun onResults(results: Bundle) {
                        if (completed) return
                        completed = true
                        val text = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                        val elapsed = SystemClock.elapsedRealtime() - startTime
                        val chunks = assembler.onFinal(text, elapsed)
                        val normalised = assembler.finalText(text)
                        recognizer.stopListening()
                        recognizer.destroy()
                        continuation.resume(
                            TranscriptResult(
                                text = normalised,
                                chunks = chunks,
                                speakers = null,
                                engine = "local",
                                langTag = langTag
                            )
                        )
                    }

                    override fun onPartialResults(partialResults: Bundle) {
                        onPartial(partialResults, startTime, assembler)
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                }
                recognizer.setRecognitionListener(listener)
                recognizer.startListening(intent)
                continuation.invokeOnCancellation {
                    recognizer.cancel()
                    recognizer.destroy()
                }
            }
        }
    }

    private fun handlePartial(
        bundle: Bundle,
        startTime: Long,
        assembler: PartialChunkAssembler,
        onPartial: (TranscriptChunk) -> Unit
    ) {
        val text = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: return
        val elapsed = SystemClock.elapsedRealtime() - startTime
        val chunk = assembler.onPartial(text, elapsed)
        if (chunk != null) {
            onPartial(chunk)
        }
    }

    private fun buildBaseIntent(lang: Locale, offlinePreferred: Boolean): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, lang.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, offlinePreferred)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }
    }

    private fun buildFileIntent(
        lang: Locale,
        descriptor: ParcelFileDescriptor,
        format: AudioFormatInfo
    ): Intent {
        return buildBaseIntent(lang, false).apply {
            putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, descriptor)
            putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_CHANNEL_COUNT, format.channelCount)
            putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
            putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE, format.sampleRate)
        }
    }

    private fun loadLanguages(): List<Locale> {
        val intent = Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS)
        val latch = CountDownLatch(1)
        val locales = mutableListOf<Locale>()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val extras = getResultExtras(true)
                val tags = extras?.getStringArrayList(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES)
                    ?: extras?.getStringArrayList(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE)
                if (tags != null) {
                    for (tag in tags) {
                        runCatching { Locale.forLanguageTag(tag) }.getOrNull()?.let { locales.add(it) }
                    }
                }
                latch.countDown()
            }
        }
        runCatching {
            context.sendOrderedBroadcast(intent, null, receiver, null, Activity.RESULT_OK, null, null)
            latch.await(300, TimeUnit.MILLISECONDS)
        }
        return locales.ifEmpty { listOf(Locale.getDefault()) }
    }

    private suspend fun resolveFormat(uri: android.net.Uri): AudioFormatInfo {
        return withContext(Dispatchers.IO) {
            val extractor = MediaExtractor()
            val afd = context.contentResolver.openAssetFileDescriptor(uri, "r")
                ?: throw IOException("Cannot open audio source")
            afd.use {
                extractor.setDataSource(it.fileDescriptor, it.startOffset, it.length)
            }
            val trackIndex = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: throw IOException("No audio track found")
            val format = extractor.getTrackFormat(trackIndex)
            val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: throw IOException("Unknown audio format")
            extractor.release()
            AudioFormatInfo(sampleRate, channelCount, mime)
        }
    }

    private suspend fun decodeToPipe(
        uri: android.net.Uri,
        format: AudioFormatInfo,
        output: java.io.OutputStream
    ) {
        withContext(Dispatchers.IO) {
            val extractor = MediaExtractor()
            val afd = context.contentResolver.openAssetFileDescriptor(uri, "r")
                ?: throw IOException("Cannot open audio source")
            afd.use {
                extractor.setDataSource(it.fileDescriptor, it.startOffset, it.length)
            }
            val trackIndex = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: throw IOException("No audio track found")
            extractor.selectTrack(trackIndex)
            val trackFormat = extractor.getTrackFormat(trackIndex)
            if (format.mime == MediaFormat.MIMETYPE_AUDIO_RAW) {
                val buffer = ByteArray(32 * 1024)
                val byteBuffer = ByteBuffer.wrap(buffer)
                while (isActive) {
                    byteBuffer.clear()
                    val size = extractor.readSampleData(byteBuffer, 0)
                    if (size < 0) break
                    output.write(buffer, 0, size)
                    extractor.advance()
                }
            } else {
                val codec = MediaCodec.createDecoderByType(format.mime)
                codec.configure(trackFormat, null, null, 0)
                codec.start()
                val bufferInfo = MediaCodec.BufferInfo()
                var endOfStream = false
                while (!endOfStream && isActive) {
                    val inputIndex = codec.dequeueInputBuffer(10_000)
                    if (inputIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputIndex) ?: continue
                        val size = extractor.readSampleData(inputBuffer, 0)
                        if (size < 0) {
                            codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        } else {
                            val presentationTimeUs = extractor.sampleTime
                            codec.queueInputBuffer(inputIndex, 0, size, presentationTimeUs, 0)
                            extractor.advance()
                        }
                    }
                    var outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000)
                    while (outputIndex >= 0) {
                        val buffer = codec.getOutputBuffer(outputIndex)
                        if (buffer != null && bufferInfo.size > 0) {
                            val bytes = ByteArray(bufferInfo.size)
                            buffer.get(bytes)
                            buffer.clear()
                            output.write(bytes)
                        }
                        endOfStream = bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        codec.releaseOutputBuffer(outputIndex, false)
                        if (endOfStream) break
                        outputIndex = codec.dequeueOutputBuffer(bufferInfo, 0)
                    }
                }
                codec.stop()
                codec.release()
            }
            extractor.release()
            output.flush()
        }
    }

    private data class AudioFormatInfo(
        val sampleRate: Int,
        val channelCount: Int,
        val mime: String
    )
}
