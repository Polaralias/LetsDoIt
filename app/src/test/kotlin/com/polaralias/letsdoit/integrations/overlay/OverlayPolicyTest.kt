package com.polaralias.letsdoit.integrations.overlay

import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayPolicyTest {

    @Test
    fun youtubeVideoInForegroundShowsFullOverlay() {
        val context = PlaybackContext(
            app = StreamingApp.YOUTUBE,
            windowState = WindowState.FULLSCREEN,
            playbackState = PlaybackState.PLAYING,
            mediaType = MediaType.VIDEO
        )

        assertEquals(OverlayBehavior.FullScreen, resolveOverlayBehavior(context))
    }

    @Test
    fun youtubeVideoInPipShowsFullOverlay() {
        val context = PlaybackContext(
            app = StreamingApp.YOUTUBE,
            windowState = WindowState.PICTURE_IN_PICTURE,
            playbackState = PlaybackState.PLAYING,
            mediaType = MediaType.VIDEO
        )

        assertEquals(OverlayBehavior.FullScreen, resolveOverlayBehavior(context))
    }

    @Test
    fun youtubePausedHidesOverlay() {
        val context = PlaybackContext(
            app = StreamingApp.YOUTUBE,
            windowState = WindowState.FULLSCREEN,
            playbackState = PlaybackState.PAUSED,
            mediaType = MediaType.VIDEO
        )

        assertEquals(OverlayBehavior.None, resolveOverlayBehavior(context))
    }

    @Test
    fun youtubeBackgroundStopsOverlay() {
        val context = PlaybackContext(
            app = StreamingApp.YOUTUBE,
            windowState = WindowState.BACKGROUND,
            playbackState = PlaybackState.PLAYING,
            mediaType = MediaType.VIDEO
        )

        assertEquals(OverlayBehavior.None, resolveOverlayBehavior(context))
    }

    @Test
    fun youtubeMusicFullscreenVideoGetsFullOverlay() {
        val context = PlaybackContext(
            app = StreamingApp.YOUTUBE_MUSIC,
            windowState = WindowState.FULLSCREEN,
            playbackState = PlaybackState.PLAYING,
            mediaType = MediaType.VIDEO
        )

        assertEquals(OverlayBehavior.FullScreen, resolveOverlayBehavior(context))
    }

    @Test
    fun youtubeMusicMiniplayerGetsPartialOverlay() {
        val context = PlaybackContext(
            app = StreamingApp.YOUTUBE_MUSIC,
            windowState = WindowState.MINIPLAYER,
            playbackState = PlaybackState.PLAYING,
            mediaType = MediaType.VIDEO
        )

        val behavior = resolveOverlayBehavior(context)
        require(behavior is OverlayBehavior.Partial)
        assertEquals(5f / 6f, behavior.heightFraction)
        assertEquals(true, behavior.allowTouchPassThrough)
    }

    @Test
    fun youtubeMusicNonFullscreenVideoGetsPartialOverlay() {
        val context = PlaybackContext(
            app = StreamingApp.YOUTUBE_MUSIC,
            windowState = WindowState.NON_FULLSCREEN,
            playbackState = PlaybackState.PLAYING,
            mediaType = MediaType.VIDEO
        )

        val behavior = resolveOverlayBehavior(context)
        require(behavior is OverlayBehavior.Partial)
        assertEquals(true, behavior.allowTouchPassThrough)
    }

    @Test
    fun youtubeMusicAudioPlaybackHasNoOverlay() {
        val context = PlaybackContext(
            app = StreamingApp.YOUTUBE_MUSIC,
            windowState = WindowState.FULLSCREEN,
            playbackState = PlaybackState.PLAYING,
            mediaType = MediaType.AUDIO
        )

        assertEquals(OverlayBehavior.None, resolveOverlayBehavior(context))
    }

    @Test
    fun youtubeMusicBackgroundHidesOverlay() {
        val context = PlaybackContext(
            app = StreamingApp.YOUTUBE_MUSIC,
            windowState = WindowState.BACKGROUND,
            playbackState = PlaybackState.PLAYING,
            mediaType = MediaType.VIDEO
        )

        assertEquals(OverlayBehavior.None, resolveOverlayBehavior(context))
    }

    @Test
    fun applierDelegatesToRenderer() {
        val renderer = FakeRenderer()
        val applier = OverlayPolicyApplier(renderer)
        val context = PlaybackContext(
            app = StreamingApp.YOUTUBE_MUSIC,
            windowState = WindowState.MINIPLAYER,
            playbackState = PlaybackState.PLAYING,
            mediaType = MediaType.VIDEO
        )

        applier.apply(context)

        assertEquals("partial", renderer.lastCall)
        assertEquals(5f / 6f, renderer.lastHeight)
        assertEquals(true, renderer.touchPassThrough)
    }

    private class FakeRenderer : OverlayRenderer {
        var lastCall: String? = null
        var lastHeight: Float? = null
        var touchPassThrough: Boolean? = null

        override fun showFullScreen() {
            lastCall = "full"
        }

        override fun showPartial(heightFraction: Float, allowTouchPassThrough: Boolean) {
            lastCall = "partial"
            lastHeight = heightFraction
            touchPassThrough = allowTouchPassThrough
        }

        override fun hide() {
            lastCall = "hide"
        }
    }
}
