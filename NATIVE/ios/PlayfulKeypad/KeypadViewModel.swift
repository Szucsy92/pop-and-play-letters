import SwiftUI
import Observation

/// Egy felugró nagy karakter a soron.
struct KidTyped: Identifiable, Equatable {
    let id: Int
    let ch: String
    let colorIdx: Int
}

/// Konfetti-robbanás. kind: 0 = kis pukkanás, 1 = nagy (az 5. karakternél), 2 = ünneplés a szövegbe repüléskor.
struct KidBurst: Identifiable {
    let id: Int
    let kind: Int
    let slot: Int
    let total: Int
    let seed: Int
    let start: Date
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
    var pending: [KidTyped] = []      // a soron felsorakozó (max 5) karakter
    var history: String = UserDefaults.standard.string(forKey: "typed_history") ?? ""  // megtartott szöveg
    var flying: [KidTyped] = []       // épp a szövegbe repülő sor
    var flyId = 0
    var flyStart = Date()
    var bump = 0
    var bursts: [KidBurst] = []
    var parentMenu = false
    var exited = false

    @ObservationIgnored private var uid = 0
    @ObservationIgnored private var taps: [Date] = []
    @ObservationIgnored private var flushTask: Task<Void, Never>?

    private let rowSize = 5
    private let maxHistory = 200

    func press(_ value: String) {
        guard let first = value.first, first != " " else { return }
        let seed = Int(first.unicodeScalars.first?.value ?? 1)
        var flushed = false
        if pending.count >= rowSize {
            doFlush()
            flushed = true
        }
        uid += 1
        pending.append(KidTyped(id: uid, ch: String(first).uppercased(), colorIdx: seed % 6))
        addBurst(kind: pending.count == rowSize ? 1 : 0, slot: pending.count - 1, total: pending.count, seed: seed)
        if soundEnabled {
            NativeSoundEngine.shared.play(seed: seed)
            if flushed { NativeSoundEngine.shared.chime() }
        }
        // Ha a sor megtelt, hamar a szövegbe repül; ha a gyerek megáll, később akkor is.
        flushTask?.cancel()
        let wait: UInt64 = pending.count >= rowSize ? 900_000_000 : 3_200_000_000
        flushTask = Task { @MainActor [weak self] in
            try? await Task.sleep(nanoseconds: wait)
            if Task.isCancelled { return }
            self?.flushNow()
        }
    }

    private func addBurst(kind: Int, slot: Int, total: Int, seed: Int) {
        uid += 1
        bursts.append(KidBurst(id: uid, kind: kind, slot: slot, total: total, seed: seed, start: Date()))
        if bursts.count > 6 { bursts.removeFirst(bursts.count - 6) }
    }

    private func doFlush() {
        guard !pending.isEmpty else { return }
        history = String((history + pending.map(\.ch).joined()).suffix(maxHistory))
        addBurst(kind: 2, slot: 0, total: 0, seed: pending[0].colorIdx)
        flying = pending
        flyId += 1
        flyStart = Date()
        bump += 1
        pending = []
        UserDefaults.standard.set(history, forKey: "typed_history")
    }

    func flushNow() {
        guard !pending.isEmpty else { return }
        doFlush()
        if soundEnabled { NativeSoundEngine.shared.chime() }
    }

    func clearText() {
        flushTask?.cancel()
        history = ""
        pending = []
        flying = []
        UserDefaults.standard.removeObject(forKey: "typed_history")
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
