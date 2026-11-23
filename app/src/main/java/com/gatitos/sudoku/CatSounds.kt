package com.gatitos.sudoku

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

enum class CatSound(val res: Int) {
    RONRONEO(R.raw.ronroneo),   // perder una vida
    GRUNIDO(R.raw.grunido),     // perder la partida
    MAULLIDO(R.raw.maullido),   // ganar
}

/** Carga los sonidos una vez y los reproduce con baja latencia. */
class CatSounds(context: Context) {
    private val pool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val ids = CatSound.entries.associateWith { pool.load(context, it.res, 1) }

    fun play(sound: CatSound) {
        pool.autoPause()
        ids[sound]?.let { pool.play(it, 1f, 1f, 1, 0, 1f) }
    }

    fun release() = pool.release()
}
