package hu.gyerekbillentyuzet.core.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.ArrayDeque
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sin

/**
 * A webes sound.ts lágy hangjai: C-dúr pentaton xilofon + "pop".
 * Puha felfutás és lecsengés (nincs kattogás), legfeljebb 4 egyszerre szóló hang,
 * 30 ms-nál sűrűbb leütésnél kihagyja a hangot.
 */
class NativeSoundEngine {
    private val sampleRate = 44100
    private val notes = doubleArrayOf(523.25, 587.33, 659.25, 783.99, 880.0, 1046.5, 1174.66, 1318.51)
    private val master = 0.55
    private val maxVoices = 4
    private val minGapMs = 30L

    private val worker = Executors.newSingleThreadScheduledExecutor()
    private val cache = HashMap<Int, ShortArray>()
    private val active = ArrayDeque<AudioTrack>()
    private var lastAt = 0L

    fun play(seed: Int, pop: Boolean = false) {
        val now = System.currentTimeMillis()
        if (now - lastAt < minGapMs) return
        lastAt = now
        worker.execute {
            try {
                val key = if (pop) -1 else abs(seed) % notes.size
                val pcm = cache.getOrPut(key) { if (pop) renderPop() else renderXylo(key) }
                while (active.size >= maxVoices) releaseTrack(active.pollFirst())
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
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
                    .setBufferSizeInBytes(pcm.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(pcm, 0, pcm.size)
                track.play()
                active.addLast(track)
                val ms = pcm.size * 1000L / sampleRate + 80
                worker.schedule({
                    if (active.remove(track)) releaseTrack(track)
                }, ms, TimeUnit.MILLISECONDS)
            } catch (_: Exception) {
            }
        }
    }

    private fun releaseTrack(track: AudioTrack?) {
        try {
            track?.stop()
        } catch (_: Exception) {
        }
        try {
            track?.release()
        } catch (_: Exception) {
        }
    }

    private fun renderXylo(idx: Int): ShortArray {
        val f = notes[idx]
        val peak = 0.3 - idx * 0.012
        val seconds = 1.2
        val count = (sampleRate * seconds).toInt()
        val out = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val env = if (t < 0.012) t / 0.012 * peak else peak * exp(-(t - 0.012) / 0.2)
            var s = sin(2 * PI * f * t) * env
            if (f < 900) {
                val oe = if (t < 0.006) t / 0.006 * 0.1 else 0.1 * exp(-(t - 0.006) / 0.04)
                s += sin(2 * PI * f * 3.99 * t) * oe * env / peak
            }
            out[i] = (s * master * fade(i, count) * 32767).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun renderPop(): ShortArray {
        val seconds = 0.35
        val count = (sampleRate * seconds).toInt()
        val out = ShortArray(count)
        var phase = 0.0
        val k = ln(240.0 / 620.0) / 0.1
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val freq = if (t < 0.1) 620.0 * exp(k * t) else 240.0
            phase += 2 * PI * freq / sampleRate
            val env = if (t < 0.008) t / 0.008 * 0.28 else 0.28 * exp(-(t - 0.008) / 0.035)
            out[i] = (sin(phase) * env * master * fade(i, count) * 32767).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    /** Az utolsó 40 ms-ban elhalkul, hogy ne legyen hirtelen vágás. */
    private fun fade(i: Int, count: Int): Double {
        val tail = (sampleRate * 0.04).toInt()
        return if (i >= count - tail) (count - i).toDouble() / tail else 1.0
    }
}
