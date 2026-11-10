import SwiftUI
import Observation

@Observable
final class KeypadViewModel {
    var mode: String = UserDefaults.standard.string(forKey: "keyboard_mode") ?? "inner" {
        didSet { UserDefaults.standard.set(mode, forKey: "keyboard_mode") }
    }
    var soundEnabled: Bool = UserDefaults.standard.object(forKey: "sound_enabled") as? Bool ?? true {
        didSet { UserDefaults.standard.set(soundEnabled, forKey: "sound_enabled") }
    }
    var character = ""
    var characterID = 0
    var parentMenu = false
    var exited = false
    private var taps: [Date] = []
    func press(_ value: String) { guard let ch=value.last else { return }; character=String(ch).uppercased(); characterID += 1; if soundEnabled { NativeSoundEngine.shared.play(seed: ch.unicodeScalars.first?.value ?? 1, pop: (ch.unicodeScalars.first?.value ?? 1)%3 == 0) } }
    func registerLockTap() {
        let now=Date(); taps=taps.filter { now.timeIntervalSince($0) <= 2 }; taps.append(now)
        if taps.count >= 5 { taps.removeAll(); parentMenu=true }
    }
}
