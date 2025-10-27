package com.polaralias.letsdoit.accent

import com.polaralias.letsdoit.security.SecurePrefs
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

@Singleton
class OpenAiImagesProvider @Inject constructor(
    private val securePrefs: SecurePrefs,
    private val moshi: Moshi
) : AccentImageProvider {
    override val id: String = ID

    private val client = OkHttpClient()

    override suspend fun generate(prompt: String, variants: Int, size: String): List<ByteArray> {
        val key = securePrefs.read("openai_key")?.takeIf { it.isNotBlank() } ?: throw AccentGenerationException.MissingApiKey
        val requestBody = OpenAiImageRequest(
            model = MODEL,
            prompt = prompt,
            size = size,
            variants = variants
        )
        val adapter = moshi.adapter(OpenAiImageRequest::class.java)
        val body = adapter.toJson(requestBody).toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(ENDPOINT)
            .addHeader("Authorization", "Bearer $key")
            .addHeader("OpenAI-Beta", "assistants=v2")
            .post(body)
            .build()
        val response = execute(request)
        val status = response.code
        if (!response.isSuccessful) {
            val message = response.body?.string()?.let { parseErrorMessage(it) }
            response.close()
            throw AccentGenerationException.ApiError(status, message)
        }
        val payload = response.body?.string().also { response.close() } ?: throw AccentGenerationException.InvalidResponse
        val parsed = moshi.adapter(OpenAiImageResponse::class.java).fromJson(payload) ?: throw AccentGenerationException.InvalidResponse
        if (parsed.error != null) {
            throw AccentGenerationException.ApiError(status, parsed.error.message)
        }
        val data = parsed.data ?: emptyList()
        if (data.isEmpty()) {
            throw AccentGenerationException.InvalidResponse
        }
        val images = data.mapNotNull { item ->
            val encoded = item.b64Json ?: return@mapNotNull null
            runCatching { Base64.getDecoder().decode(encoded) }.getOrNull()
        }
        if (images.isEmpty()) {
            throw AccentGenerationException.InvalidResponse
        }
        return images
    }

    private suspend fun execute(request: Request): Response {
        return runCatching {
            withContext(Dispatchers.IO) { client.newCall(request).execute() }
        }.getOrElse {
            throw AccentGenerationException.NetworkError
        }
    }

    private fun parseErrorMessage(payload: String): String? {
        return runCatching {
            val adapter = moshi.adapter(OpenAiErrorWrapper::class.java)
            adapter.fromJson(payload)?.error?.message
        }.getOrNull()
    }

    companion object {
        const val ID = "openai_images"
        private const val MODEL = "gpt-image-1"
        private const val ENDPOINT = "https://api.openai.com/v1/images/generations"
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}

data class OpenAiImageRequest(
    val model: String,
    val prompt: String,
    val size: String,
    @Json(name = "n") val variants: Int,
    @Json(name = "response_format") val responseFormat: String = "b64_json"
)

data class OpenAiImageResponse(
    val data: List<OpenAiImageData>?,
    val error: OpenAiError? = null
)

data class OpenAiImageData(
    @Json(name = "b64_json") val b64Json: String?
)

data class OpenAiErrorWrapper(
    val error: OpenAiError?
)

data class OpenAiError(
    val message: String?
)
