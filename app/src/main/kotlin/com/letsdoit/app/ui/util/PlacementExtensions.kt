package com.letsdoit.app.ui.util

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.animateItemPlacement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.debouncedItemPlacement(key: Any, debounceMillis: Long = 200L): Modifier {
    var animate by remember { mutableStateOf(false) }
    LaunchedEffect(key) {
        animate = true
        delay(debounceMillis)
        animate = false
    }
    return if (animate) {
        this.animateItemPlacement()
    } else {
        this
    }
}
