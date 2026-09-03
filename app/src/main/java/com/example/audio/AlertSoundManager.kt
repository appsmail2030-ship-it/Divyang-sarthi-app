package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

class AlertSoundManager(private val context: Context) {
    private var vibrator: Vibrator? = null
    private var isPlayingSiren = false
    private var sirenJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    @Synchronized
    fun playSiren(durationMillis: Long = 20_000L) {
        if (isPlayingSiren) return
        isPlayingSiren = true

        startVibration()

        sirenJob = scope.launch {
            val sampleRate = 44100
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = maxOf(minBufferSize, sampleRate / 2)

            var audioTrack: AudioTrack? = null
            try {
                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
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
                    .setBufferSizeInBytes(bufferSize * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack.play()

                val startTime = System.currentTimeMillis()
                val chunk = ShortArray(bufferSize)
                var phase = 0.0

                // Oscillate frequency between 700Hz and 1250Hz for realistic metro operational siren
                var freq = 700.0
                var goingUp = true

                while (isActive && isPlayingSiren) {
                    if (System.currentTimeMillis() - startTime > durationMillis) {
                        break
                    }

                    for (i in chunk.indices) {
                        if (goingUp) {
                            freq += 0.03
                            if (freq >= 1250.0) goingUp = false
                        } else {
                            freq -= 0.03
                            if (freq <= 700.0) goingUp = true
                        }

                        val angularFreq = 2.0 * Math.PI * freq / sampleRate
                        phase += angularFreq
                        if (phase > 2.0 * Math.PI) phase -= 2.0 * Math.PI

                        // Sine wave with high amplitude for loud alert
                        chunk[i] = (sin(phase) * 32000.0).toInt().coerceIn(-32767, 32767).toShort()
                    }

                    audioTrack.write(chunk, 0, chunk.size)
                }
            } catch (e: Exception) {
                Log.e("AlertSoundManager", "Error in siren generation: ${e.message}")
            } finally {
                try {
                    audioTrack?.stop()
                    audioTrack?.release()
                } catch (e: Exception) {
                    // Ignore release errors
                }
                stopVibration()
                isPlayingSiren = false
            }
        }
    }

    @Synchronized
    fun stopSiren() {
        isPlayingSiren = false
        sirenJob?.cancel()
        sirenJob = null
        stopVibration()
    }

    private fun startVibration() {
        try {
            val timings = longArrayOf(0, 500, 200, 500, 200, 800)
            val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(timings, amplitudes, 0)
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(timings, 0)
            }
        } catch (e: Exception) {
            Log.e("AlertSoundManager", "Error starting vibration: ${e.message}")
        }
    }

    private fun stopVibration() {
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e("AlertSoundManager", "Error stopping vibration: ${e.message}")
        }
    }
}
