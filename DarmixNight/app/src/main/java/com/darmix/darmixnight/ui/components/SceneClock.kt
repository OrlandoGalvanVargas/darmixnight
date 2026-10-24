package com.darmix.darmixnight.ui.components

import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.withFrameNanos

@Composable
fun rememberSceneClock(): State<Float> = produceState(0f) {
    val origin = withFrameNanos { it }
    while (true) {
        withInfiniteAnimationFrameNanos { value = (it - origin) / 1_000_000_000f }
    }
}
