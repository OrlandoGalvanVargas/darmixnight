package com.darmix.darmixnight.ui.components

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

internal class SoundBank(context: Context, resources: IntArray) {

    private val pool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val ids = resources.associateWith { pool.load(context, it, 1) }

    fun play(resource: Int, volume: Float = 1f, rate: Float = 1f) {
        val id = ids[resource] ?: return
        pool.play(id, volume, volume, 1, 0, rate.coerceIn(0.5f, 2f))
    }

    fun release() = pool.release()
}

@Composable
internal fun rememberSoundBank(vararg resources: Int): SoundBank {
    val context = LocalContext.current
    val bank = remember { SoundBank(context.applicationContext, resources) }
    DisposableEffect(bank) {
        onDispose { bank.release() }
    }
    return bank
}
