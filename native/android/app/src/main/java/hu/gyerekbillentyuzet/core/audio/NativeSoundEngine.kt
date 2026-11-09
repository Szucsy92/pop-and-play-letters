package hu.gyerekbillentyuzet.core.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.sin

/** Lightweight synthesized tones; starts on a dedicated thread to avoid UI blocking. */
class NativeSoundEngine {
    private val sampleRate = 44100
    fun play(seed: Int, pop: Boolean = false) {
        thread(name = "kid-audio") {
            val duration = if (pop) 0.12 else 0.28
            val count = (sampleRate * duration).toInt()
            val buffer = ShortArray(count)
            val freq = if (pop) 620.0 else listOf(523.25,587.33,659.25,783.99,880.0,1046.5,1174.66,1318.51)[kotlin.math.abs(seed) % 8]
            for (i in buffer.indices) {
                val t = i.toDouble() / sampleRate
                val envelope = if (pop) (1.0 - i.toDouble()/count).coerceAtLeast(0.0) else kotlin.math.exp(-t * 7)
                val f = if (pop) freq * (1.0 - 0.6*t/duration) else freq
                buffer[i] = (sin(2 * PI * f * t) * envelope * 8000).toInt().toShort()
            }
            val track = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(sampleRate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(buffer.size * 2).setTransferMode(AudioTrack.MODE_STATIC).build()
            track.write(buffer, 0, buffer.size)
            track.play()
            Thread.sleep((duration * 1000).toLong() + 30)
            track.release()
        }
    }
}
