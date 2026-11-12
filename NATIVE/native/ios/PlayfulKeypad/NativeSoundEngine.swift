import AVFoundation

/// A webes sound.ts lágy hangjai: C-dúr pentaton xilofon + "pop".
/// Puha felfutás és lecsengés (nincs kattogás), legfeljebb 4 egyszerre szóló hang,
/// 30 ms-nál sűrűbb leütésnél kihagyja a hangot.
final class NativeSoundEngine {
    static let shared = NativeSoundEngine()

    private let engine = AVAudioEngine()
    private var players: [AVAudioPlayerNode] = []
    private var nextPlayer = 0
    private let sampleRate = 44100.0
    private let format: AVAudioFormat
    private var cache: [Int: AVAudioPCMBuffer] = [:]
    private var lastAt = Date.distantPast
    private let notes: [Double] = [523.25, 587.33, 659.25, 783.99, 880.0, 1046.5, 1174.66, 1318.51]
    private let master = 0.55

    private init() {
        format = AVAudioFormat(standardFormatWithSampleRate: 44100.0, channels: 1)!
        for _ in 0..<4 {
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

    func play(seed: UInt32, pop: Bool) {
        let now = Date()
        if now.timeIntervalSince(lastAt) < 0.03 { return }
        lastAt = now
        let key = pop ? -1 : Int(seed) % notes.count
        let buffer: AVAudioPCMBuffer
        if let cached = cache[key] {
            buffer = cached
        } else {
            guard let made = pop ? renderPop() : renderXylo(key) else { return }
            cache[key] = made
            buffer = made
        }
        if !engine.isRunning { try? engine.start() }
        let player = players[nextPlayer]
        nextPlayer = (nextPlayer + 1) % players.count
        player.scheduleBuffer(buffer, at: nil, options: .interrupts, completionHandler: nil)
        if !player.isPlaying { player.play() }
    }

    private func makeBuffer(seconds: Double, fill: (Double, Int, Int) -> Double) -> AVAudioPCMBuffer? {
        let count = AVAudioFrameCount(sampleRate * seconds)
        guard let buffer = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: count),
              let data = buffer.floatChannelData?[0] else { return nil }
        buffer.frameLength = count
        let total = Int(count)
        let tail = Int(sampleRate * 0.04)
        for i in 0..<total {
            let t = Double(i) / sampleRate
            let fade = i >= total - tail ? Double(total - i) / Double(tail) : 1.0
            data[i] = Float(fill(t, i, total) * master * fade)
        }
        return buffer
    }

    private func renderXylo(_ idx: Int) -> AVAudioPCMBuffer? {
        let f = notes[idx]
        let peak = 0.3 - Double(idx) * 0.012
        return makeBuffer(seconds: 1.2) { t, _, _ in
            let env = t < 0.012 ? t / 0.012 * peak : peak * exp(-(t - 0.012) / 0.2)
            var s = sin(2 * Double.pi * f * t) * env
            if f < 900 {
                let oe = t < 0.006 ? t / 0.006 * 0.1 : 0.1 * exp(-(t - 0.006) / 0.04)
                s += sin(2 * Double.pi * f * 3.99 * t) * oe * env / peak
            }
            return s
        }
    }

    private func renderPop() -> AVAudioPCMBuffer? {
        let k = log(240.0 / 620.0) / 0.1
        var phase = 0.0
        return makeBuffer(seconds: 0.35) { t, _, _ in
            let freq = t < 0.1 ? 620.0 * exp(k * t) : 240.0
            phase += 2 * Double.pi * freq / 44100.0
            let env = t < 0.008 ? t / 0.008 * 0.28 : 0.28 * exp(-(t - 0.008) / 0.035)
            return sin(phase) * env
        }
    }
}
