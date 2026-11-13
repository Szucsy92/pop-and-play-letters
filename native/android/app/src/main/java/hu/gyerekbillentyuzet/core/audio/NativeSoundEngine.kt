package hu.gyerekbillentyuzet.core.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh
import kotlin.random.Random

/**
 * Kellemes zongora és xilofon hangok, hangfájlok nélkül szintetizálva.
 *
 * Egyetlen folyamatos AudioTrack-re keverjük a hangokat (saját keverő szál), így:
 *  - egyszerre tetszőleges számú hang szólhat (akkordok, egyszerre nyomott gombok),
 *  - nincs hangeldobás gyors egymás utáni vagy egyidejű gombnyomásnál,
 *  - a sok hang együtt sem vágódik el (lágy limiter).
 *
 * A hangok C-dúr pentatonban szólnak, így bármilyen kombináció összecseng.
 * A "beíró" hang (amikor a sor a szövegbe repül) mindig egy másik, 3-4 hangos zongora-futam.
 */
class NativeSoundEngine {
    private val sampleRate = 44100
    private val pianoNotes = doubleArrayOf(261.63, 293.66, 329.63, 392.0, 440.0, 523.25, 587.33, 659.25, 783.99, 880.0)
    private val master = 0.6f
    private val chunk = 512 // ~11.6 ms-os keverési blokk

    /** Beíró dallamok: a pentaton hangsor lépései (0..5), mindegyik 3 vagy 4 hang. */
    private val chimePatterns = arrayOf(
        intArrayOf(0, 2, 4),
        intArrayOf(0, 1, 3),
        intArrayOf(4, 2, 3),
        intArrayOf(0, 2, 1, 4),
        intArrayOf(0, 1, 2, 4),
        intArrayOf(3, 1, 2, 5),
        intArrayOf(0, 3, 2, 5),
        intArrayOf(2, 4, 3, 0),
        intArrayOf(1, 3, 2, 4),
    )

    private class Voice(val pcm: FloatArray, val gain: Float, var delay: Int) {
        var pos = 0
    }

    private val incoming = LinkedBlockingQueue<Voice>()
    private val cache = ConcurrentHashMap<Int, FloatArray>()
    private val worker = Executors.newSingleThreadExecutor()
    private val rnd = Random(System.nanoTime())
    private var lastPattern = -1

    @Volatile
    private var running = true

    private val mixer = Thread({ mixLoop() }, "kid-mixer").apply {
        isDaemon = true
        priority = Thread.MAX_PRIORITY
        start()
    }

    init {
        // A hangokat előre elkészítjük, hogy az első gombnyomásnál se legyen késés.
        Thread {
            try {
                for (i in pianoNotes.indices) {
                    sample(true, i)
                    sample(false, i)
                }
            } catch (_: Throwable) {
            }
        }.apply { isDaemon = true; start() }
    }

    /** Egy betűhöz tartozó hang: a betű kódja dönt a hangszerről és a hangmagasságról. Nincs korlátozás: egyszerre több is szólhat. */
    fun play(seed: Int) {
        val s = abs(seed)
        val idx = s % pianoNotes.size
        val piano = s % 3 != 0 // 2/3 zongora, 1/3 xilofon
        worker.execute { enqueue(sample(piano, idx), 1f, 0) }
    }

    /** Beíró hang: 3 vagy 4 kellemes zongorahang, minden alkalommal más sorrend és magasság. */
    fun chime() {
        worker.execute {
            var p = rnd.nextInt(chimePatterns.size)
            if (p == lastPattern) p = (p + 1 + rnd.nextInt(chimePatterns.size - 1)) % chimePatterns.size
            lastPattern = p
            val pattern = chimePatterns[p]
            val top = pattern.maxOrNull() ?: 0
            val base = rnd.nextInt(pianoNotes.size - top) // 0..(9 - top)
            val step = (sampleRate * (0.095 + rnd.nextDouble() * 0.04)).toInt()
            pattern.forEachIndexed { n, off ->
                val vel = 0.62f + rnd.nextFloat() * 0.22f
                enqueue(sample(true, base + off), vel, n * step)
            }
        }
    }

    fun shutdown() {
        running = false
        incoming.clear()
        worker.shutdown()
    }

    private fun enqueue(pcm: FloatArray, gain: Float, delayFrames: Int) {
        if (running) incoming.offer(Voice(pcm, gain, delayFrames))
    }

    private fun sample(piano: Boolean, idx: Int): FloatArray {
        val key = idx + if (piano) 0 else 100
        cache[key]?.let { return it }
        val made = if (piano) renderPiano(pianoNotes[idx]) else renderXylo(pianoNotes[idx] * 2)
        cache[key] = made
        return made
    }

    // ---------------------------------------------------------------- keverő

    private fun mixLoop() {
        var track: AudioTrack? = null
        try {
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
            )
            val t = AudioTrack.Builder()
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
                .setBufferSizeInBytes(max(minBuf, chunk * 2 * 3))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .build()
            track = t

            val voices = ArrayList<Voice>()
            val mix = FloatArray(chunk)
            val out = ShortArray(chunk)
            var playing = false

            while (running) {
                if (voices.isEmpty()) {
                    val first = incoming.poll(250, TimeUnit.MILLISECONDS)
                    if (first == null) {
                        // Üresjárat: leállítjuk a lejátszást, hogy ne fogyassza az akkut.
                        if (playing) {
                            try { t.pause(); t.flush() } catch (_: Exception) {}
                            playing = false
                        }
                        continue
                    }
                    voices.add(first)
                }
                incoming.drainTo(voices)
                if (!playing) {
                    t.play()
                    playing = true
                }

                java.util.Arrays.fill(mix, 0f)
                for (v in voices) {
                    var i = 0
                    if (v.delay > 0) {
                        if (v.delay >= chunk) {
                            v.delay -= chunk
                            continue
                        }
                        i = v.delay
                        v.delay = 0
                    }
                    val pcm = v.pcm
                    var p = v.pos
                    val g = v.gain
                    while (i < chunk && p < pcm.size) {
                        mix[i] += pcm[p] * g
                        i++
                        p++
                    }
                    v.pos = p
                }
                voices.removeAll { it.pos >= it.pcm.size }

                for (i in 0 until chunk) {
                    // lágy limiter: sok egyidejű hangnál sem torzít, sem vágódik
                    val y = tanh(mix[i] * master)
                    out[i] = (y * 32767f).toInt().coerceIn(-32767, 32767).toShort()
                }
                t.write(out, 0, chunk)
            }
        } catch (_: Throwable) {
        } finally {
            try { track?.pause() } catch (_: Throwable) {}
            try { track?.flush() } catch (_: Throwable) {}
            try { track?.release() } catch (_: Throwable) {}
        }
    }

    // ---------------------------------------------------------------- hangszintézis

    /** Lágy zongora: halk felhangok, nagyon halk, puha kalapács-koppanás, finom felfutás. */
    private fun renderPiano(f: Double): FloatArray {
        val count = (sampleRate * 2.2).toInt()
        val out = FloatArray(count)
        val amps = doubleArrayOf(1.0, 0.45, 0.20, 0.09, 0.04, 0.02, 0.01)
        val tau1 = (1.4 - 0.7 * ((f - 261.63) / (880.0 - 261.63))).coerceIn(0.6, 1.4)
        var noise = 12345L
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            var s = 0.0
            for (k in 1..7) {
                val fk = f * k * sqrt(1.0 + 0.0003 * k * k)
                val tauK = tau1 / (1.0 + 0.6 * (k - 1))
                s += amps[k - 1] * sin(2 * PI * fk * t) * exp(-t / tauK)
            }
            s += 0.35 * sin(2 * PI * f * 1.0008 * t) * exp(-t / tau1)
            noise = (noise * 1103515245L + 12345L) and 0x7fffffffL
            s += (noise / 2147483648.0 * 2 - 1) * 0.04 * exp(-t / 0.012)
            val attack = if (t < 0.008) t / 0.008 else 1.0
            out[i] = (s * attack * 0.20 * tailFade(i, count)).toFloat()
        }
        return out
    }

    private fun renderXylo(f: Double): FloatArray {
        val count = (sampleRate * 1.3).toInt()
        val out = FloatArray(count)
        var noise = 54321L
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            var s = sin(2 * PI * f * t) * exp(-t / 0.45)
            s += 0.22 * sin(2 * PI * f * 3.0 * t) * exp(-t / 0.12)
            s += 0.05 * sin(2 * PI * f * 6.2 * t) * exp(-t / 0.04)
            noise = (noise * 1103515245L + 12345L) and 0x7fffffffL
            s += (noise / 2147483648.0 * 2 - 1) * 0.04 * exp(-t / 0.004)
            val attack = if (t < 0.004) t / 0.004 else 1.0
            out[i] = (s * attack * 0.28 * tailFade(i, count)).toFloat()
        }
        return out
    }

    /** Az utolsó 60 ms-ban halkítás, hogy ne legyen kattanás a hang végén. */
    private fun tailFade(i: Int, count: Int): Double {
        val tail = (sampleRate * 0.06).toInt()
        return if (i >= count - tail) (count - i).toDouble() / tail else 1.0
    }
}
