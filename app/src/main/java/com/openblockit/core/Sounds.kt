package com.openblockit.core

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.sin

object Sounds {
    const val OFF = 0
    const val RAIN = 1
    const val WAVES = 2
    const val WHITE = 3
    const val CITY = 4

    private var track: AudioTrack? = null
    private var worker: Thread? = null

    @Volatile
    private var running = false

    @Volatile
    private var fade = 1f

    fun start(context: Context, mode: Int) {
        stop()
        if (mode == OFF) return
        val sampleRate = 44100
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(8192)
        val t = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBuf * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (_: Exception) {
            return
        }
        track = t
        running = true
        fade = 1f
        try {
            t.play()
        } catch (_: Exception) {
            stop()
            return
        }
        worker = thread(name = "openblockit-sound") {
            val buf = ShortArray(minBuf / 2)
            var brown = 0f
            var phase = 0.0
            val rnd = java.util.Random()
            while (running) {
                for (i in buf.indices) {
                    val v: Float = when (mode) {
                        RAIN -> {
                            brown = (brown + (rnd.nextFloat() - 0.5f) * 0.02f) * 0.995f
                            brown * 2.5f
                        }
                        WAVES -> {
                            phase += 0.00002
                            val lfo = (sin(phase) * 0.5 + 0.5).toFloat()
                            brown = (brown + (rnd.nextFloat() - 0.5f) * 0.03f) * 0.99f
                            brown * (0.8f + lfo)
                        }
                        WHITE -> (rnd.nextFloat() - 0.5f) * 0.35f
                        CITY -> {
                            brown = (brown + (rnd.nextFloat() - 0.5f) * 0.01f) * 0.999f
                            brown * 1.8f
                        }
                        else -> 0f
                    }
                    buf[i] = (v * fade * 6000f).toInt().coerceIn(-32768, 32767).toShort()
                }
                try {
                    t.write(buf, 0, buf.size)
                } catch (_: Exception) {
                    break
                }
            }
        }
    }

    fun update(secondsLeft: Int) {
        fade = when {
            secondsLeft <= 20 -> 0f
            secondsLeft <= 60 -> (secondsLeft - 20) / 40f
            else -> 1f
        }
    }

    fun stop() {
        running = false
        try {
            worker?.join(500L)
        } catch (_: Exception) {
        }
        worker = null
        try {
            track?.pause()
            track?.flush()
            track?.release()
        } catch (_: Exception) {
        }
        track = null
        fade = 1f
    }
}
