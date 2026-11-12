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
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Kellemes zongora és xilofon hangok, hangfájlok nélkül szintetizálva.
 * - Zongora: 7 felhang finom inharmonicitással, két húr enyhe hangolás-eltéréssel, kalapács-koppanással,
 *   a magasabb felhangok gyorsabban csengenek le.
 * - Xilofon: tiszta alaphang + gyorsan elhaló felhangok és egy halk ütő-koppanás.
 * A hangok C-dúr pentatonban szólnak, így bármilyen gyors gépelés is kellemes marad.
 */
class NativeSoundEngine {
    private val sampleRate = 44100
    private val pianoNotes = doubleArrayOf(261.63, 293.66, 329.63, 392.0, 440.0, 523.25, 587.33, 659.25, 783.99, 880.0)
    private val master = 0.55
    private val maxVoices = 6
    private val minGapMs = 30L

    private val worker = Executors.newSingleThreadScheduledExecutor()
    private val cache = HashMap<Int, ShortArray>()
    private val active = ArrayDeque<AudioTrack>()
    private var lastAt = 0L

    /** Egy betűhöz tartozó hang: a betű kódja dönt a hangszerről és a hangmagasságról. */
    fun play(seed: Int) {
        val now = System.currentTimeMillis()
        if (now - lastAt < minGapMs) return
        lastAt = now
        val idx = abs(seed) % pianoNotes.size
        val piano = abs(seed) % 3 != 0 // 2/3 zongora, 1/3 xilofon
        worker.execute { voice(piano, idx) }
    }

    /** Kis felfelé futó dallam, amikor a sor a szövegbe repül. */
    fun chime() {
        for (i in 0 until 5) {
            worker.schedule({ voice(i % 2 == 0, 5 + i) }, i * 75L, TimeUnit.MILLISECONDS)
        }
    }

    fun shutdown() {
        worker.execute {
            while (active.isNotEmpty()) releaseTrack(active.pollFirst())
        }
        worker.shutdown()
    }

    private fun voice(piano: Boolean, idx: Int) {
        try {
            val key = idx + if (piano) 0 else 100
            val pcm = cache.getOrPut(key) { if (piano) renderPiano(pianoNotes[idx]) else renderXylo(pianoNotes[idx] * 2) }
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
            worker.schedule({ if (active.remove(track)) releaseTrack(track) }, ms, TimeUnit.MILLISECONDS)
        } catch (_: Exception) {
        }
    }

    private fun releaseTrack(track: AudioTrack?) {
        try { track?.stop() } catch (_: Exception) {}
        try { track?.release() } catch (_: Exception) {}
    }

    private fun renderPiano(f: Double): ShortArray {
        val count = (sampleRate * 2.2).toInt()
        val out = ShortArray(count)
        val amps = doubleArrayOf(1.0, 0.55, 0.32, 0.18, 0.10, 0.06, 0.03)
        val tau1 = (1.4 - 0.7 * ((f - 261.63) / (880.0 - 261.63))).coerceIn(0.6, 1.4)
        var noise = 12345L
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            var s = 0.0
            for (k in 1..7) {
                val fk = f * k * sqrt(1.0 + 0.0003 * k * k)
                val tauK = tau1 / (1.0 + 0.5 * (k - 1))
                s += amps[k - 1] * sin(2 * PI * fk * t) * exp(-t / tauK)
            }
            s += 0.35 * sin(2 * PI * f * 1.0008 * t) * exp(-t / tau1)
            noise = (noise * 1103515245L + 12345L) and 0x7fffffffL
            s += (noise / 2147483648.0 * 2 - 1) * 0.25 * exp(-t / 0.006)
            val attack = if (t < 0.004) t / 0.004 else 1.0
            out[i] = pcm(s * attack * 0.17, i, count)
        }
        return out
    }

    private fun renderXylo(f: Double): ShortArray {
        val count = (sampleRate * 1.3).toInt()
        val out = ShortArray(count)
        var noise = 54321L
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            var s = sin(2 * PI * f * t) * exp(-t / 0.45)
            s += 0.3 * sin(2 * PI * f * 3.0 * t) * exp(-t / 0.14)
            s += 0.12 * sin(2 * PI * f * 6.2 * t) * exp(-t / 0.05)
            noise = (noise * 1103515245L + 12345L) and 0x7fffffffL
            s += (noise / 2147483648.0 * 2 - 1) * 0.12 * exp(-t / 0.003)
            val attack = if (t < 0.002) t / 0.002 else 1.0
            out[i] = pcm(s * attack * 0.32, i, count)
        }
        return out
    }

    /** Master hangerő + az utolsó 60 ms-ban halkítás, hogy ne legyen kattanás a végén. */
    private fun pcm(v: Double, i: Int, count: Int): Short {
        val tail = (sampleRate * 0.06).toInt()
        val fade = if (i >= count - tail) (count - i).toDouble() / tail else 1.0
        return (v * master * fade * 32767).toInt().coerceIn(-32767, 32767).toShort()
    }
}
