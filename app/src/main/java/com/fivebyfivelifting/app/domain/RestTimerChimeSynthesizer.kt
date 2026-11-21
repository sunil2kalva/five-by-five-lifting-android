package com.fivebyfivelifting.app.domain

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Pure-Kotlin PCM synthesizer that generates a crisp, prominent, ascending multi-harmonic
 * gym timer alert chime (A-major dual cadence centered in the 880–3326 Hz sensitivity band).
 *
 * Unlike default alarm ringtones (which often fade in softly for several seconds on Android)
 * or low-amplitude ToneGenerator beeps, this buffer hits full loudness immediately so it cuts
 * unmistakably above ducked background music (e.g. Spotify) on both headphones and speakers.
 */
object RestTimerChimeSynthesizer {
    const val SAMPLE_RATE_HZ: Int = 44_100
    const val DURATION_MS: Int = 2_750

    private const val PEAK_SCALE: Double = 0.94
    private const val DRIVE: Double = 1.6
    private const val ATTACK_SECONDS: Double = 0.008
    private const val RELEASE_SECONDS: Double = 0.030

    private data class Partial(val freqHz: Double, val weight: Double)

    private val noteA5 = listOf(
        Partial(880.0, 0.58),
        Partial(1760.0, 0.28),
        Partial(2640.0, 0.14)
    )
    private val noteCSharp6 = listOf(
        Partial(1108.73, 0.58),
        Partial(2217.46, 0.28),
        Partial(3326.19, 0.14)
    )
    private val chordE6 = listOf(
        Partial(1318.51, 0.45),
        Partial(880.0, 0.30),
        Partial(2637.02, 0.25)
    )
    private val finalChord = listOf(
        Partial(1318.51, 0.38),
        Partial(1760.0, 0.34),
        Partial(880.0, 0.28)
    )

    fun buildChimePcm(): ShortArray {
        val totalSamples = (SAMPLE_RATE_HZ.toLong() * DURATION_MS / 1000L).toInt()
        val mix = DoubleArray(totalSamples)

        // First ascending cadence: A5 - A5 - C#6 - E6 chord
        addTone(mix, startMs = 0, durationMs = 170, partials = noteA5, decayRate = 1.2)
        addTone(mix, startMs = 260, durationMs = 170, partials = noteA5, decayRate = 1.2)
        addTone(mix, startMs = 520, durationMs = 170, partials = noteCSharp6, decayRate = 1.2)
        addTone(mix, startMs = 780, durationMs = 480, partials = chordE6, decayRate = 1.8)

        // Brief breathing gap (1260..1540 ms), then second cadence: A5 - C#6 - bright bell chord
        addTone(mix, startMs = 1540, durationMs = 170, partials = noteA5, decayRate = 1.2)
        addTone(mix, startMs = 1800, durationMs = 170, partials = noteCSharp6, decayRate = 1.2)
        addTone(mix, startMs = 2060, durationMs = 640, partials = finalChord, decayRate = 1.6)

        val pcm = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val clamped = mix[i].coerceIn(-1.0, 1.0)
            val scaled = (clamped * PEAK_SCALE * Short.MAX_VALUE).roundToInt()
            pcm[i] = scaled.coerceIn(-Short.MAX_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return pcm
    }

    private fun addTone(
        buffer: DoubleArray,
        startMs: Int,
        durationMs: Int,
        partials: List<Partial>,
        decayRate: Double
    ) {
        val startIdx = (startMs.toLong() * SAMPLE_RATE_HZ / 1000L).toInt()
        val sampleCount = (durationMs.toLong() * SAMPLE_RATE_HZ / 1000L).toInt()
        if (sampleCount <= 0 || startIdx >= buffer.size) return

        val attackSamples = minOf((ATTACK_SECONDS * SAMPLE_RATE_HZ).toInt(), sampleCount / 3).coerceAtLeast(1)
        val releaseSamples = minOf((RELEASE_SECONDS * SAMPLE_RATE_HZ).toInt(), sampleCount / 2).coerceAtLeast(1)
        val normTanh = tanh(DRIVE)
        val twoPi = 2.0 * PI

        for (i in 0 until sampleCount) {
            val idx = startIdx + i
            if (idx >= buffer.size) break
            val t = i.toDouble() / SAMPLE_RATE_HZ

            var envelope = when {
                i < attackSamples -> 0.5 * (1.0 - cos(PI * i / attackSamples))
                i >= sampleCount - releaseSamples -> {
                    val relIdx = i - (sampleCount - releaseSamples)
                    0.5 * (1.0 + cos(PI * relIdx / releaseSamples))
                }
                else -> 1.0
            }
            if (decayRate > 0.0) {
                envelope *= exp(-decayRate * t)
            }

            var raw = 0.0
            for (partial in partials) {
                raw += partial.weight * sin(twoPi * partial.freqHz * t)
            }
            val shaped = tanh(raw * DRIVE) / normTanh
            buffer[idx] += shaped * envelope
        }
    }
}
