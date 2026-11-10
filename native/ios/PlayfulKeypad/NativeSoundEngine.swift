import AVFoundation

/// Synthesizes soft tones without bundling audio assets.
final class NativeSoundEngine {
    static let shared = NativeSoundEngine()
    private let engine = AVAudioEngine()
    private let player = AVAudioPlayerNode()
    private let sampleRate = 44100.0
    private init() {
        engine.attach(player)
        engine.connect(player, to: engine.mainMixerNode, format: AVAudioFormat(standardFormatWithSampleRate: sampleRate, channels: 1))
        try? engine.start()
    }
    func play(seed: UInt32, pop: Bool) {
        let duration = pop ? 0.12 : 0.28
        let count = AVAudioFrameCount(sampleRate * duration)
        guard let format=AVAudioFormat(standardFormatWithSampleRate: sampleRate, channels: 1),
              let buffer=AVAudioPCMBuffer(pcmFormat: format, frameCapacity: count),
              let data=buffer.floatChannelData?[0] else { return }
        let notes=[523.25,587.33,659.25,783.99,880.0,1046.5,1174.66,1318.51]
        let freq=pop ? 620.0 : notes[Int(seed)%notes.count]
        buffer.frameLength=count
        for i in 0..<Int(count) {
            let t=Double(i)/sampleRate
            let env=pop ? max(0,1-Double(i)/Double(count)) : exp(-t*7)
            let f=pop ? freq*(1-0.6*t/duration) : freq
            data[i]=Float(sin(2*Double.pi*f*t)*env*0.18)
        }
        if !engine.isRunning { try? engine.start() }
        player.scheduleBuffer(buffer)
        if !player.isPlaying { player.play() }
    }
}
