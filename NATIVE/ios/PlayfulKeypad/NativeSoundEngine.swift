import AVFoundation

/// Kellemes zongora és xilofon hangok, hangfájlok nélkül szintetizálva.
/// - Zongora: 7 felhang finom inharmonicitással, két húr enyhe hangolás-eltéréssel, kalapács-koppanással,
///   a magasabb felhangok gyorsabban csengenek le.
/// - Xilofon: tiszta alaphang + gyorsan elhaló felhangok és egy halk ütő-koppanás.
/// A hangok C-dúr pentatonban szólnak, így bármilyen gyors gépelés is kellemes marad.
final class NativeSoundEngine {
    static let shared = NativeSoundEngine()

    private let engine = AVAudioEngine()
    private var players: [AVAudioPlayerNode] = []
    private var nextPlayer = 0
    private let sampleRate = 44100.0
    private let format: AVAudioFormat
    private var cache: [Int: AVAudioPCMBuffer] = [:]
    private var lastAt = Date.distantPast
    private let pianoNotes: [Double] = [261.63, 293.66, 329.63, 392.0, 440.0, 523.25, 587.33, 659.25, 783.99, 880.0]
    private let master = 0.55

    private init() {
        format = AVAudioFormat(standardFormatWithSampleRate: 44100.0, channels: 1)!
        for _ in 0..<6 {
            let p = AVAudioPlayerNode()
            engine.attach(p)
            engine.connect(p, to: engine.mainMixerNode, format: format)
            players.append(p)
        }
        try? AVAudioSession.sharedInstance().setCategory(.playback, options: [.mixWithOthers])
        try? AVAudioSession.sharedInstance().setActive(true)
        engine.prepare()
        try? engine.start()
    }

    /// Egy betűhöz tartozó hang: a betű kódja dönt a hangszerről és a hangmagasságról.
    func play(seed: Int) {
        let now = Date()
        if now.timeIntervalSince(lastAt) < 0.03 { return }
        lastAt = now
        voice(piano: abs(seed) % 3 != 0, idx: abs(seed) % pianoNotes.count) // 2/3 zongora, 1/3 xilofon
    }

    /// Kis felfelé futó dallam, amikor a sor a szövegbe repül.
    func chime() {
        for i in 0..<5 {
            DispatchQueue.main.asyncAfter(deadline: .now() + Double(i) * 0.075) { [weak self] in
                self?.voice(piano: i % 2 == 0, idx: 5 + i)
            }
        }
    }

    private func voice(piano: Bool, idx: Int) {
        let key = idx + (piano ? 0 : 100)
        let buffer: AVAudioPCMBuffer
        if let cached = cache[key] {
            buffer = cached
        } else {
            guard let made = piano ? renderPiano(pianoNotes[idx]) : renderXylo(pianoNotes[idx] * 2) else { return }
            cache[key] = made
            buffer = made
        }
        if !engine.isRunning { try? engine.start() }
        let player = players[nextPlayer]
        nextPlayer = (nextPlayer + 1) % players.count
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

    private func renderPiano(_ f: Double) -> AVAudioPCMBuffer? {
        let amps: [Double] = [1.0, 0.55, 0.32, 0.18, 0.10, 0.06, 0.03]
        let tau1 = min(1.4, max(0.6, 1.4 - 0.7 * ((f - 261.63) / (880.0 - 261.63))))
        var noise: UInt64 = 12345
        return makeBuffer(seconds: 2.2) { t in
            var s = 0.0
            for k in 1...7 {
                let kd = Double(k)
                let fk = f * kd * (1.0 + 0.0003 * kd * kd).squareRoot()
                let tauK = tau1 / (1.0 + 0.5 * (kd - 1))
                s += amps[k - 1] * sin(2 * Double.pi * fk * t) * exp(-t / tauK)
            }
            s += 0.35 * sin(2 * Double.pi * f * 1.0008 * t) * exp(-t / tau1)
            noise = (noise &* 1103515245 &+ 12345) & 0x7fffffff
            s += (Double(noise) / 2147483648.0 * 2 - 1) * 0.25 * exp(-t / 0.006)
            let attack = t < 0.004 ? t / 0.004 : 1.0
            return s * attack * 0.17
        }
    }

    private func renderXylo(_ f: Double) -> AVAudioPCMBuffer? {
        var noise: UInt64 = 54321
        return makeBuffer(seconds: 1.3) { t in
            var s = sin(2 * Double.pi * f * t) * exp(-t / 0.45)
            s += 0.3 * sin(2 * Double.pi * f * 3.0 * t) * exp(-t / 0.14)
            s += 0.12 * sin(2 * Double.pi * f * 6.2 * t) * exp(-t / 0.05)
            noise = (noise &* 1103515245 &+ 12345) & 0x7fffffff
            s += (Double(noise) / 2147483648.0 * 2 - 1) * 0.12 * exp(-t / 0.003)
            let attack = t < 0.002 ? t / 0.002 : 1.0
            return s * attack * 0.32
        }
    }
}
