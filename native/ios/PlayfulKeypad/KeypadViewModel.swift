import SwiftUI
import Observation

/// A képernyő közepén látszó betű (a webes `Shown`).
struct KidShown: Equatable {
    let id: Int
    let ch: String
    let colorIdx: Int
    let anim: Int
    let start: Date
}

/// Egy konfetti / csavarodó darab (a webes `Bit`).
struct KidBit: Identifiable {
    let id: Int
    let dx: Double
    let dy: Double
    let r: Double
    let colorIdx: Int
    let curl: Bool
}

@Observable
final class KeypadViewModel {
    /// nil = még nem választott módot, ilyenkor a választó látszik (mint a weben).
    var mode: String? = UserDefaults.standard.string(forKey: "kidkb-mode") {
        didSet {
            if let mode { UserDefaults.standard.set(mode, forKey: "kidkb-mode") }
        }
    }
    var soundEnabled: Bool = UserDefaults.standard.object(forKey: "sound_enabled") as? Bool ?? true {
        didSet { UserDefaults.standard.set(soundEnabled, forKey: "sound_enabled") }
    }
    var shown: KidShown?
    var bits: [KidBit] = []
    var parentMenu = false
    var exited = false

    private var uid = 0
    private var taps: [Date] = []

    func press(_ value: String) {
        guard let first = value.first, first != " " else { return }
        let seed = Int(first.unicodeScalars.first?.value ?? 1)
        uid += 1
        shown = KidShown(id: uid, ch: String(first).uppercased(), colorIdx: seed % 6, anim: seed % 4, start: Date())
        var list: [KidBit] = []
        for i in 0..<22 {
            let ang = Double.random(in: 0..<(Double.pi * 2))
            let dist = 120 + Double.random(in: 0..<260)
            uid += 1
            list.append(KidBit(id: uid, dx: cos(ang) * dist, dy: sin(ang) * dist,
                               r: Double.random(in: -360..<360), colorIdx: (i + seed) % 6, curl: i % 5 == 0))
        }
        bits = list
        if soundEnabled {
            NativeSoundEngine.shared.play(seed: UInt32(seed), pop: seed % 3 == 0)
        }
    }

    func toggleMode() { mode = (mode == "inner") ? "native" : "inner" }

    /// Lakat: 5 gyors koppintás (2 mp-en belül) nyitja a szülői menüt.
    func registerLockTap() {
        let now = Date()
        taps = taps.filter { now.timeIntervalSince($0) <= 2 }
        taps.append(now)
        if taps.count >= 5 {
            taps.removeAll()
            parentMenu = true
        }
    }
}
