package com.polaralias.letsdoit.ui.components.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToLong

@Composable
fun StandardPlaybackControls(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    positionMillis: Long,
    durationMillis: Long,
    bufferedMillis: Long = positionMillis,
    onPlayPauseToggle: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onSeekTo: (Long) -> Unit
) {
    val clampedDuration = durationMillis.coerceAtLeast(0L)
    val sliderRange = if (clampedDuration > 0) 0f..clampedDuration.toFloat() else 0f..1f
    val sliderValue = when {
        clampedDuration <= 0 -> 0f
        else -> positionMillis.coerceIn(0, clampedDuration).toFloat()
    }
    val bufferedFraction = remember(bufferedMillis, clampedDuration) {
        if (clampedDuration <= 0) 0f else bufferedMillis.coerceIn(0, clampedDuration).toFloat() / clampedDuration.toFloat()
    }

    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        BufferingSlider(
            value = sliderValue,
            range = sliderRange,
            enabled = clampedDuration > 0,
            bufferFraction = bufferedFraction,
            onSeek = onSeekTo
        )
        Spacer(modifier = Modifier.height(8.dp))
        TimeLabels(
            positionMillis = positionMillis,
            durationMillis = durationMillis
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(onClick = onSkipPrevious, modifier = Modifier.size(48.dp)) {
                Icon(imageVector = Icons.Filled.SkipPrevious, contentDescription = "Skip previous")
            }
            Spacer(modifier = Modifier.width(24.dp))
            IconButton(onClick = onPlayPauseToggle, modifier = Modifier.size(64.dp)) {
                val icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow
                val description = if (isPlaying) "Pause" else "Play"
                Icon(imageVector = icon, contentDescription = description)
            }
            Spacer(modifier = Modifier.width(24.dp))
            IconButton(onClick = onSkipNext, modifier = Modifier.size(48.dp)) {
                Icon(imageVector = Icons.Filled.SkipNext, contentDescription = "Skip next")
            }
        }
    }
}

@Composable
private fun BufferingSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    bufferFraction: Float,
    onSeek: (Long) -> Unit
) {
    val sliderColors = SliderDefaults.colors(
        thumbColor = MaterialTheme.colorScheme.primary,
        activeTrackColor = MaterialTheme.colorScheme.primary,
        inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer,
        disabledActiveTrackColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        disabledThumbColor = MaterialTheme.colorScheme.secondaryContainer
    )

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        LinearProgressIndicator(
            progress = { bufferFraction.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            color = MaterialTheme.colorScheme.secondaryContainer
        )
        Slider(
            value = value,
            onValueChange = { onSeek(it.roundToLong()) },
            valueRange = range,
            enabled = enabled,
            steps = 0,
            colors = sliderColors,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun TimeLabels(
    positionMillis: Long,
    durationMillis: Long
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = formatTimestamp(positionMillis), style = MaterialTheme.typography.labelMedium)
        Text(text = formatTimestamp(durationMillis), style = MaterialTheme.typography.labelMedium)
    }
}

private fun formatTimestamp(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
