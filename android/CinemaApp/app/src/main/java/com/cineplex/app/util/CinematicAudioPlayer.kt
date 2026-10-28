package com.cineplex.app.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Generates an offline, procedural cinematic subwoofer bass drop / rumble
 * using low-latency AudioTrack PCM synthesis.
 * Does not require external downloads or permissions and respects device ringer mode.
 */
object CinematicAudioPlayer {

    private var currentTrack: AudioTrack? = null

    suspend fun playSubwooferBass(context: Context, isMuted: Boolean) {
        if (isMuted) return

        withContext(Dispatchers.Default) {
            try {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                if (audioManager != null) {
                    val ringerMode = audioManager.ringerMode
                    if (ringerMode == AudioManager.RINGER_MODE_SILENT || ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
                        return@withContext
                    }
                }

                stop()

                val sampleRate = 44100
                val durationSec = 1.1
                val numSamples = (sampleRate * durationSec).toInt()
                val samples = ShortArray(numSamples)

                // Subwoofer Bass Drop: 72 Hz sweeping down to 34 Hz with subtle punch and warmth
                var phase1 = 0.0
                var phase2 = 0.0

                for (i in 0 until numSamples) {
                    val progress = i.toDouble() / numSamples
                    val freq1 = 72.0 - (progress * 38.0) // 72Hz -> 34Hz
                    val freq2 = freq1 * 1.5              // Sub-harmonic overtone

                    phase1 += 2.0 * PI * freq1 / sampleRate
                    phase2 += 2.0 * PI * freq2 / sampleRate

                    // Attack: 30ms punch; Decay: smooth exponential cinema subwoofer roll-off
                    val timeSec = i.toDouble() / sampleRate
                    val envelope = when {
                        timeSec < 0.03 -> (timeSec / 0.03)
                        else -> exp(-(timeSec - 0.03) * 2.8)
                    }

                    // Combined harmonic wave with soft saturation
                    val rawWave = 0.85 * sin(phase1) + 0.25 * sin(phase2)
                    // Soft clipping limiter
                    val clamped = rawWave.coerceIn(-1.0, 1.0)
                    samples[i] = (clamped * envelope * Short.MAX_VALUE * 0.88).toInt().toShort()
                }

                val bufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(samples.size * 2)

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(samples, 0, samples.size)
                currentTrack = track
                track.play()
            } catch (_: Exception) {
                // Audio synthesis error safely ignored to ensure visual splash remains unaffected
            }
        }
    }

    fun stop() {
        try {
            currentTrack?.let {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    it.stop()
                }
                it.release()
            }
            currentTrack = null
        } catch (_: Exception) {
            currentTrack = null
        }
    }
}
