package com.example.eleven

import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import kotlin.math.log10

class GlobalBoostEngine {

    enum class Mode { LOUDNESS_ENHANCER, EQUALIZER_FALLBACK, UNSUPPORTED }

    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var equalizer: Equalizer? = null

    var mode: Mode = Mode.UNSUPPORTED
        private set

    fun start(percent: Int): Mode {
        release()

        val gainMb = percentToMillibels(percent)

        try {
            loudnessEnhancer = LoudnessEnhancer(0).apply {
                setTargetGain(gainMb)
                enabled = true
            }
            mode = Mode.LOUDNESS_ENHANCER
            return mode
        } catch (_: Throwable) {
            loudnessEnhancer?.release()
            loudnessEnhancer = null
        }

        try {
            equalizer = Equalizer(1000, 0).apply {
                val range = bandLevelRange
                val safeGain = gainMb.coerceIn(range[0].toInt(), range[1].toInt()).toShort()
                for (band in 0 until numberOfBands) {
                    setBandLevel(band.toShort(), safeGain)
                }
                enabled = true
            }
            mode = Mode.EQUALIZER_FALLBACK
            return mode
        } catch (_: Throwable) {
            equalizer?.release()
            equalizer = null
        }

        mode = Mode.UNSUPPORTED
        return mode
    }

    fun setPercent(percent: Int) {
        val gainMb = percentToMillibels(percent)

        loudnessEnhancer?.let {
            try {
                it.setTargetGain(gainMb)
                if (!it.enabled) it.enabled = true
            } catch (_: Throwable) {
            }
        }

        equalizer?.let { eq ->
            try {
                val range = eq.bandLevelRange
                val safeGain = gainMb.coerceIn(range[0].toInt(), range[1].toInt()).toShort()
                for (band in 0 until eq.numberOfBands) {
                    eq.setBandLevel(band.toShort(), safeGain)
                }
                if (!eq.enabled) eq.enabled = true
            } catch (_: Throwable) {
            }
        }
    }

    fun release() {
        try { loudnessEnhancer?.enabled = false } catch (_: Throwable) {}
        try { loudnessEnhancer?.release() } catch (_: Throwable) {}
        loudnessEnhancer = null

        try { equalizer?.enabled = false } catch (_: Throwable) {}
        try { equalizer?.release() } catch (_: Throwable) {}
        equalizer = null

        mode = Mode.UNSUPPORTED
    }

    companion object {
        fun percentToDb(percent: Int): Double {
            val ratio = percent.coerceIn(100, 200) / 100.0
            return 20.0 * log10(ratio)
        }

        fun percentToMillibels(percent: Int): Int =
            (percentToDb(percent) * 100.0).toInt()
    }
}
