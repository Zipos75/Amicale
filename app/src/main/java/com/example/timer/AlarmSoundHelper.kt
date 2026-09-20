package com.example.timer

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

object AlarmSoundHelper {

    private const val TAG = "AlarmSoundHelper"

    /**
     * Plays the official end-of-period buzzer/referee whistle on USAGE_ALARM stream
     * so it sounds even if phone is in silent/do-not-disturb mode,
     * accompanied by heavy vibration.
     */
    fun playPeriodEndAlarm(context: Context) {
        val appContext = context.applicationContext

        // 1. Play Alarm Sound on USAGE_ALARM stream
        playAlarmAudio(appContext)

        // 2. Trigger heavy vibration pattern
        triggerVibration(appContext)
    }

    /**
     * Plays player substitution double beep + vibration alert for player rotations
     */
    fun playSubstitutionBeep(context: Context) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val toneGen = android.media.ToneGenerator(android.media.AudioManager.STREAM_ALARM, 100)
                toneGen.startTone(android.media.ToneGenerator.TONE_PROP_BEEP2, 600)
            } catch (_: Exception) {
                playSynthesizedSubstitutionBlasts()
            }
        }
        triggerSubstitutionVibration(appContext)
    }

    private fun playSynthesizedSubstitutionBlasts() {
        try {
            val sampleRate = 44100
            val blast1 = generateWhistleSamples(sampleRate, 0.2, 3000.0)
            val silence = ByteArray((sampleRate * 0.1 * 2).toInt())
            val blast2 = generateWhistleSamples(sampleRate, 0.35, 3000.0)

            val totalBytes = blast1.size + silence.size + blast2.size
            val buffer = ByteArray(totalBytes)
            var offset = 0
            System.arraycopy(blast1, 0, buffer, offset, blast1.size)
            offset += blast1.size
            System.arraycopy(silence, 0, buffer, offset, silence.size)
            offset += silence.size
            System.arraycopy(blast2, 0, buffer, offset, blast2.size)

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            val track = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(buffer.size)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()
        } catch (e: Exception) {
            Log.e(TAG, "AudioTrack substitution blast error", e)
        }
    }

    private fun playAlarmAudio(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Try playing system alarm ringtone first
                val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(audioAttributes)
                    setDataSource(context, alarmUri)
                    isLooping = false
                    prepare()
                    start()
                }

                mediaPlayer.setOnCompletionListener {
                    it.release()
                }
            } catch (e: Exception) {
                Log.w(TAG, "MediaPlayer failed, falling back to synthesized whistle on USAGE_ALARM", e)
                playSynthesizedWhistle()
            }
        }
    }

    /**
     * Synthesized referee whistle blasts (3 blasts: short, short, long)
     * using AudioTrack with USAGE_ALARM.
     */
    private fun playSynthesizedWhistle() {
        try {
            val sampleRate = 44100
            val blast1 = generateWhistleSamples(sampleRate, 0.4, 2800.0)
            val silence1 = ByteArray((sampleRate * 0.15 * 2).toInt())
            val blast2 = generateWhistleSamples(sampleRate, 0.4, 2800.0)
            val silence2 = ByteArray((sampleRate * 0.15 * 2).toInt())
            val blast3 = generateWhistleSamples(sampleRate, 1.2, 2800.0)

            val totalBytes = blast1.size + silence1.size + blast2.size + silence2.size + blast3.size
            val buffer = ByteArray(totalBytes)
            var offset = 0

            System.arraycopy(blast1, 0, buffer, offset, blast1.size)
            offset += blast1.size
            System.arraycopy(silence1, 0, buffer, offset, silence1.size)
            offset += silence1.size
            System.arraycopy(blast2, 0, buffer, offset, blast2.size)
            offset += blast2.size
            System.arraycopy(silence2, 0, buffer, offset, silence2.size)
            offset += silence2.size
            System.arraycopy(blast3, 0, buffer, offset, blast3.size)

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            val track = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(buffer.size)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()
        } catch (e: Exception) {
            Log.e(TAG, "AudioTrack synthesis error", e)
        }
    }

    private fun generateWhistleSamples(sampleRate: Int, durationSec: Double, freqHz: Double): ByteArray {
        val numSamples = (sampleRate * durationSec).toInt()
        val bytes = ByteArray(numSamples * 2)
        var byteIdx = 0
        for (i in 0 until numSamples) {
            val time = i.toDouble() / sampleRate
            // Slight frequency modulation to mimic referee whistle trill (25Hz vibrato)
            val modFreq = freqHz + 120.0 * sin(2.0 * Math.PI * 25.0 * time)
            val angle = 2.0 * Math.PI * modFreq * time
            val sampleVal = (sin(angle) * 32000.0).toInt().toShort()

            bytes[byteIdx++] = (sampleVal.toInt() and 0xFF).toByte()
            bytes[byteIdx++] = ((sampleVal.toInt() shr 8) and 0xFF).toByte()
        }
        return bytes
    }

    private fun triggerVibration(context: Context) {
        try {
            val timings = longArrayOf(0, 500, 200, 500, 200, 1000)
            val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vibratorManager?.vibrate(CombinedVibration.createParallel(effect))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                    vibrator?.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(timings, -1)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vibration failed", e)
        }
    }

    private fun triggerSubstitutionVibration(context: Context) {
        try {
            val timings = longArrayOf(0, 200, 100, 300)
            val amplitudes = intArrayOf(0, 255, 0, 255)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vibratorManager?.vibrate(CombinedVibration.createParallel(effect))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                    vibrator?.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(timings, -1)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Substitution vibration failed", e)
        }
    }
}
