package com.polaralias.letsdoit.integrations.overlay

/**
 * Enumeration of the supported streaming applications that can render an overlay.
 */
enum class StreamingApp {
    YOUTUBE,
    YOUTUBE_MUSIC
}

/**
 * The state of the host window where playback occurs.
 */
enum class WindowState {
    FULLSCREEN,
    MINIMIZED,
    PICTURE_IN_PICTURE,
    MINIPLAYER,
    NON_FULLSCREEN,
    BACKGROUND
}

/**
 * The type of content that is currently active on the session.
 */
enum class MediaType {
    VIDEO,
    AUDIO
}

/**
 * The playback state reported by the remote session.
 */
enum class PlaybackState {
    PLAYING,
    PAUSED,
    STOPPED
}

/**
 * Describes the playback context that drives overlay decisions.
 */
data class PlaybackContext(
    val app: StreamingApp,
    val windowState: WindowState,
    val playbackState: PlaybackState,
    val mediaType: MediaType
)

sealed class OverlayBehavior {
    data object None : OverlayBehavior()
    data object FullScreen : OverlayBehavior()
    data class Partial(
        val heightFraction: Float,
        val allowTouchPassThrough: Boolean
    ) : OverlayBehavior()
}

private const val DEFAULT_PARTIAL_HEIGHT = 5f / 6f

/**
 * Resolves the overlay behaviour for the supplied playback context based on the policy matrix.
 */
fun resolveOverlayBehavior(context: PlaybackContext): OverlayBehavior {
    if (context.windowState == WindowState.BACKGROUND || context.playbackState != PlaybackState.PLAYING) {
        return OverlayBehavior.None
    }
    if (context.mediaType != MediaType.VIDEO) {
        return OverlayBehavior.None
    }

    return when (context.app) {
        StreamingApp.YOUTUBE -> OverlayBehavior.FullScreen
        StreamingApp.YOUTUBE_MUSIC -> when (context.windowState) {
            WindowState.FULLSCREEN -> OverlayBehavior.FullScreen
            WindowState.MINIPLAYER,
            WindowState.NON_FULLSCREEN,
            WindowState.MINIMIZED,
            WindowState.PICTURE_IN_PICTURE -> OverlayBehavior.Partial(
                heightFraction = DEFAULT_PARTIAL_HEIGHT,
                allowTouchPassThrough = true
            )
            WindowState.BACKGROUND -> OverlayBehavior.None
        }
    }
}

/**
 * Applies the policy to the provided renderer.
 */
class OverlayPolicyApplier(private val renderer: OverlayRenderer) {
    fun apply(context: PlaybackContext) {
        when (val behavior = resolveOverlayBehavior(context)) {
            OverlayBehavior.FullScreen -> renderer.showFullScreen()
            is OverlayBehavior.Partial -> renderer.showPartial(
                heightFraction = behavior.heightFraction,
                allowTouchPassThrough = behavior.allowTouchPassThrough
            )
            OverlayBehavior.None -> renderer.hide()
        }
    }
}

interface OverlayRenderer {
    fun showFullScreen()
    fun showPartial(heightFraction: Float, allowTouchPassThrough: Boolean)
    fun hide()
}
