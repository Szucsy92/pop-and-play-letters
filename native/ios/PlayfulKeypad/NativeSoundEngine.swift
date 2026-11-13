import AVFoundation

/// Kellemes zongora és xilofon hangok, hangfájlok nélkül szintetizálva.
/// - Zongora: lágy felhangok finom inharmonicitással, két húr enyhe hangolás-eltéréssel, nagyon puha kalapács-koppanással.
/// - Xilofon: tiszta alaphang + gyorsan elhaló felhangok.
/// A hangok C-dúr pentatonban szólnak, így bármilyen kombináció összecseng.
/// Egyszerre több hang is szólhat (nincs hangeldobás), a "beíró" hang pedig mindig egy másik 3-4 hangos zongora-futam.
final class NativeSoundEngine {
    static let shared = NativeSoundEngine()

    private let engine = AVAudioEngine()
    private var players: [AVAudioPlayerNode] = []
    private var nextPlayer = 0
    private let sampleRate = 44100.0
    private let format: AVAudioFormat
    private var cache: [Int: AVAudioPCMBuffer] = [:]
    private let cacheLock = NSLock()
    private let pianoNotes: [Double] = [261.63, 293.66, 329.63, 392.0, 440.0, 523.25, 587.33, 659.25, 783.99, 880.0]
    private let master = 0.5
    private let voiceCount = 16

    /// Beíró dallamok: a pentaton hangsor lépései (0...5), mindegyik 3 vagy 4 hang.
    private let chimePatterns: [[Int]] = [
        [0, 2, 4], [0, 1, 3], [4, 2, 3],
        [0, 2, 1, 4], [0, 1, 2, 4], [3, 1, 2, 5],
        [0, 3, 2, 5], [2, 4, 3, 0], [1, 3, 2, 4],
    ]
    private var lastPattern = -1

    private init() {
        format = AVAudioFormat(standardFormatWithSampleRate: 44100.0, channels: 1)!
        for _ in 0..<voiceCount {
            let p = AVAudioPlayerNode()
            engine.attach(p)
            engine.connect(p, to: engine.mainMixerNode, format: format)
            players.append(p)
        }
        try? AVAudioSession.sharedInstance().setCategory(.playback, options: [.mixWithOthers])
        try? AVAudioSession.sharedInstance().setActive(true)
        engine.prepare()
        try? engine.start()

        // A hangokat előre elkészítjük, hogy az első gombnyomásnál se legyen késés.
        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            guard let self else { return }
            for i in 0..<self.pianoNotes.count {
                _ = self.buffer(piano: true, idx: i)
                _ = self.buffer(piano: false, idx: i)
            }
        }
    }

    /// Egy betűhöz tartozó hang: a betű kódja dönt a hangszerről és a hangmagasságról. Nincs korlátozás, több is szólhat egyszerre.
    func play(seed: Int) {
        let s = abs(seed)
        voice(piano: s % 3 != 0, idx: s % pianoNotes.count, gain: 1.0) // 2/3 zongora, 1/3 xilofon
    }

    /// Beíró hang: 3 vagy 4 kellemes zongorahang, minden alkalommal más sorrend és magasság.
    func chime() {
        var p = Int.random(in: 0..<chimePatterns.count)
        if p == lastPattern { p = (p + 1 + Int.random(in: 0..<(chimePatterns.count - 1))) % chimePatterns.count }
        lastPattern = p
        let pattern = chimePatterns[p]
        let top = pattern.max() ?? 0
        let base = Int.random(in: 0...(pianoNotes.count - 1 - top))
        let step = Double.random(in: 0.095...0.135)
        for (n, off) in pattern.enumerated() {
            let vel = Float.random(in: 0.62...0.84)
            DispatchQueue.main.asyncAfter(deadline: .now() + Double(n) * step) { [weak self] in
                self?.voice(piano: true, idx: base + off, gain: vel)
            }
        }
    }

    private func buffer(piano: Bool, idx: Int) -> AVAudioPCMBuffer? {
        let key = idx + (piano ? 0 : 100)
        cacheLock.lock()
        let hit = cache[key]
        cacheLock.unlock()
        if let hit { return hit }
        guard let made = piano ? renderPiano(pianoNotes[idx]) : renderXylo(pianoNotes[idx] * 2) else { return nil }
        cacheLock.lock()
        cache[key] = made
        cacheLock.unlock()
        return made
    }

    private func voice(piano: Bool, idx: Int, gain: Float) {
        guard let buffer = buffer(piano: piano, idx: idx) else { return }
        if !engine.isRunning { try? engine.start() }
        let player = players[nextPlayer]
        nextPlayer = (nextPlayer + 1) % players.count
        player.volume = gain
        player.scheduleBuffer(buffer, at: nil, options: .interrupts, completionHandler: nil)
        if !player.isPlaying { player.play() }
    }

    private func makeBuffer(seconds: Double, fill: (Double) -> Double) -> AVAudioPCMBuffer? {
        let count = AVAudioFrameCount(sampleRate * seconds)
        guard let buffer = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: count),
              let data = buffer.floatChannelData?[0] else { return nil }
        buffer.frameLength = count
        let total = Int(count)
        let tail = Int(sampleRate * 0.06)
        for i in 0..<total {
            let t = Double(i) / sampleRate
            let fade = i >= total - tail ? Double(total - i) / Double(tail) : 1.0
            data[i] = Float(fill(t) * master * fade)
        }
        return buffer
    }

    /// Lágy zongora: halk felhangok, nagyon halk puha koppanás, finom felfutás.
    private func renderPiano(_ f: Double) -> AVAudioPCMBuffer? {
        let amps: [Double] = [1.0, 0.45, 0.20, 0.09, 0.04, 0.02, 0.01]
        let tau1 = min(1.4, max(0.6, 1.4 - 0.7 * ((f - 261.63) / (880.0 - 261.63))))
        var noise: UInt64 = 12345
        return makeBuffer(seconds: 2.2) { t in
            var s = 0.0
            for k in 1...7 {
                let kd = Double(k)
                let fk = f * kd * (1.0 + 0.0003 * kd * kd).squareRoot()
                let tauK = tau1 / (1.0 + 0.6 * (kd - 1))
                s += amps[k - 1] * sin(2 * Double.pi * fk * t) * exp(-t / tauK)
            }
            s += 0.35 * sin(2 * Double.pi * f * 1.0008 * t) * exp(-t / tau1)
            noise = (noise &* 1103515245 &+ 12345) & 0x7fffffff
            s += (Double(noise) / 2147483648.0 * 2 - 1) * 0.04 * exp(-t / 0.012)
            let attack = t < 0.008 ? t / 0.008 : 1.0
            return s * attack * 0.20
        }
    }

    private func renderXylo(_ f: Double) -> AVAudioPCMBuffer? {
        var noise: UInt64 = 54321
        return makeBuffer(seconds: 1.3) { t in
            var s = sin(2 * Double.pi * f * t) * exp(-t / 0.45)
            s += 0.22 * sin(2 * Double.pi * f * 3.0 * t) * exp(-t / 0.12)
            s += 0.05 * sin(2 * Double.pi * f * 6.2 * t) * exp(-t / 0.04)
            noise = (noise &* 1103515245 &+ 12345) & 0x7fffffff
            s += (Double(noise) / 2147483648.0 * 2 - 1) * 0.04 * exp(-t / 0.004)
            let attack = t < 0.004 ? t / 0.004 : 1.0
            return s * attack * 0.28
        }
    }
}
