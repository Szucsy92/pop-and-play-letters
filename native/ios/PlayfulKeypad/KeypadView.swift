import SwiftUI
import UIKit

// ---- Téma: a webes styles.css oklch-színei sRGB-ben ----
private enum Kid {
    static let background = Color(red: 19 / 255, green: 20 / 255, blue: 40 / 255)
    static let backgroundCenter = Color(red: 38 / 255, green: 36 / 255, blue: 69 / 255)
    static let foreground = Color(red: 240 / 255, green: 241 / 255, blue: 249 / 255)
    static let primary = Color(red: 95 / 255, green: 211 / 255, blue: 127 / 255)
    static let secondary = Color(red: 46 / 255, green: 48 / 255, blue: 76 / 255)
    static let muted = Color(red: 160 / 255, green: 163 / 255, blue: 184 / 255)
    static let destructive = Color(red: 231 / 255, green: 0, blue: 11 / 255)
    static let destructiveForeground = Color(red: 248 / 255, green: 250 / 255, blue: 252 / 255)
}

private let kidColors: [Color] = [
    Color(red: 255 / 255, green: 112 / 255, blue: 106 / 255),
    Color(red: 255 / 255, green: 195 / 255, blue: 26 / 255),
    Color(red: 104 / 255, green: 219 / 255, blue: 112 / 255),
    Color(red: 0, green: 190 / 255, blue: 250 / 255),
    Color(red: 218 / 255, green: 116 / 255, blue: 241 / 255),
    Color(red: 255 / 255, green: 159 / 255, blue: 95 / 255),
]

private let keypadRows: [[String]] = [
    Array("1234567890").map(String.init),
    Array("QWERTZUIOP").map(String.init),
    Array("ASDFGHJKLÉ").map(String.init),
    Array("YXCVBNMÖÜÁ").map(String.init),
    ["?", "!", "+", "-", "★", "♥", "☺", "."],
]

private func kidFont(_ size: CGFloat) -> Font {
    .system(size: size, weight: .bold, design: .rounded)
}

// ---- Animációs segédek ----
private struct CubicBezier {
    let x1: Double, y1: Double, x2: Double, y2: Double

    private func bez(_ t: Double, _ a: Double, _ b: Double) -> Double {
        3 * (1 - t) * (1 - t) * t * a + 3 * (1 - t) * t * t * b + t * t * t
    }

    func value(_ x: Double) -> Double {
        if x <= 0 { return 0 }
        if x >= 1 { return 1 }
        var lo = 0.0, hi = 1.0, t = x
        for _ in 0..<24 {
            if bez(t, x1, x2) < x { lo = t } else { hi = t }
            t = (lo + hi) / 2
        }
        return bez(t, y1, y2)
    }
}

private let launchEase = CubicBezier(x1: 0.5, y1: -0.4, x2: 0.8, y2: 0.6)

// ---- Fő nézet ----
struct KeypadView: View {
    @State private var vm = KeypadViewModel()
    @State private var systemInput = ""
    @FocusState private var keyboardFocused: Bool

    var body: some View {
        GeometryReader { geo in
            let w = geo.size.width
            let h = geo.size.height
            let btnW = min(w * 0.9, 360)
            ZStack {
                if vm.exited {
                    exitedView(btnW: btnW)
                } else {
                    gameView(w: w, h: h)
                    if vm.mode == nil { chooser(btnW: btnW) }
                    if vm.parentMenu { parentMenuView(btnW: btnW) }
                }
            }
            .frame(width: w, height: h)
        }
        .ignoresSafeArea(.container)
        .background { KidBackground().ignoresSafeArea() }
        .preferredColorScheme(.dark)
        .statusBarHidden(true)
        .persistentSystemOverlays(.hidden)
        .onAppear { setGuidedAccess(true) }
        .onChange(of: keyboardFocused) { _, focused in
            if !focused { refocusIfNeeded() }
        }
        .onChange(of: vm.mode) { _, _ in refocusIfNeeded() }
        .onChange(of: vm.parentMenu) { _, _ in refocusIfNeeded() }
    }

    private func refocusIfNeeded() {
        guard vm.mode == "native", !vm.parentMenu, !vm.exited else { return }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.12) {
            if vm.mode == "native", !vm.parentMenu, !vm.exited { keyboardFocused = true }
        }
    }

    /// iOS-en a Guided Access csak felügyelt eszközön / Single App Mode-ban kapcsolható programból;
    /// egyébként a hívás hatástalan, ilyenkor a szülő a Guided Access-t kézzel indítja.
    private func setGuidedAccess(_ on: Bool) {
        UIAccessibility.requestGuidedAccessSession(enabled: on) { _ in }
    }

    @ViewBuilder
    private func gameView(w: CGFloat, h: CGFloat) -> some View {
        let inner = vm.mode == "inner"
        ZStack {
            // színpad: begépelt szöveg, felsorakozó karakterek, konfetti
            StageView(vm: vm, w: w, h: inner ? h * 0.54 : h)
                .frame(width: w, height: inner ? h * 0.54 : h)
                .frame(width: w, height: h, alignment: .top)

            // telefon billentyűzete
            if vm.mode == "native" && !vm.parentMenu {
                TextField("", text: $systemInput)
                    .textInputAutocapitalization(.characters)
                    .autocorrectionDisabled()
                    .focused($keyboardFocused)
                    .frame(width: 1, height: 1)
                    .opacity(0.01)
                    .frame(width: w, height: h, alignment: .bottomLeading)
                    .onChange(of: systemInput) { _, value in
                        if let ch = value.last { vm.press(String(ch)) }
                        if !value.isEmpty { systemInput = "" }
                    }
                    .onAppear { keyboardFocused = true }
            }

            // nagy színes gombok
            if inner {
                keysView(w: w)
                    .frame(width: w, height: h * 0.414)
                    .frame(width: w, height: h, alignment: .bottom)
            }

            // lakat
            Text("🔒")
                .font(.system(size: 24))
                .frame(width: 48, height: 48)
                .opacity(0.4)
                .contentShape(Rectangle())
                .onLongPressGesture(minimumDuration: 0, maximumDistance: 60, pressing: { down in
                    if down { vm.registerLockTap() }
                }, perform: {})
                .padding(.top, 12)
                .padding(.trailing, 12)
                .frame(width: w, height: h, alignment: .topTrailing)
        }
        .frame(width: w, height: h)
        .contentShape(Rectangle())
        .onTapGesture {
            if vm.mode == "native" && !vm.parentMenu { keyboardFocused = true }
        }
    }

    private func keysView(w: CGFloat) -> some View {
        let fontSize = min(32, max(16, w * 0.05))
        return GeometryReader { geometry in
            let verticalPadding: CGFloat = 24
            let spacing: CGFloat = 6 * CGFloat(keypadRows.count - 1)
            let unit = max(1, (geometry.size.height - verticalPadding - spacing) / 4.6)
            VStack(spacing: 6) {
                ForEach(0..<keypadRows.count, id: \.self) { ri in
                    HStack(spacing: 6) {
                        ForEach(0..<keypadRows[ri].count, id: \.self) { ki in
                            let key = keypadRows[ri][ki]
                            KidKey(label: key, color: kidColors[(ri * 3 + ki) % kidColors.count], size: fontSize) {
                                vm.press(key)
                            }
                        }
                    }
                    .frame(height: unit * (ri == keypadRows.count - 1 ? 1 : 0.9))
                }
            }
            .padding(EdgeInsets(top: 8, leading: 8, bottom: 16, trailing: 8))
        }
    }

    private func overlayPanel<Content: View>(@ViewBuilder _ content: () -> Content) -> some View {
        VStack(spacing: 18) { content() }
            .padding(24)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Kid.background.opacity(0.95))
            .contentShape(Rectangle())
            .onTapGesture {}
    }

    private func chooser(btnW: CGFloat) -> some View {
        overlayPanel {
            Text("Melyik billentyűzet legyen?")
                .font(kidFont(30)).foregroundStyle(Kid.foreground).multilineTextAlignment(.center)
            KidButton(text: "🎨 Nagy színes gombok", bg: Kid.primary, fg: Kid.background, width: btnW) {
                vm.mode = "inner"
            }
            KidButton(text: "📱 Telefon billentyűzete", bg: Kid.secondary, fg: Kid.foreground, width: btnW) {
                vm.mode = "native"
            }
            Text("Kilépés és beállítások: 5× gyors koppintás a jobb felső lakatra.")
                .font(kidFont(14)).foregroundStyle(Kid.muted).multilineTextAlignment(.center)
                .frame(maxWidth: 320)
        }
    }

    private func parentMenuView(btnW: CGFloat) -> some View {
        overlayPanel {
            Text("Szülői menü").font(kidFont(30)).foregroundStyle(Kid.foreground)
            KidButton(text: "Billentyűzet: \(vm.mode == "inner" ? "nagy gombok" : "telefoné") ↔",
                      bg: Kid.secondary, fg: Kid.foreground, width: btnW) { vm.toggleMode() }
            KidButton(text: "Hang: \(vm.soundEnabled ? "be 🔊" : "ki 🔇")",
                      bg: Kid.secondary, fg: Kid.foreground, width: btnW) { vm.soundEnabled.toggle() }
            KidButton(text: "Begépelt szöveg törlése 🗑", bg: Kid.secondary, fg: Kid.foreground, width: btnW) {
                vm.clearText()
            }
            KidButton(text: "Vissza a játékhoz", bg: Kid.primary, fg: Kid.background, width: btnW) {
                vm.parentMenu = false
            }
            KidButton(text: "Kilépés", bg: Kid.destructive, fg: Kid.destructiveForeground, width: btnW) {
                setGuidedAccess(false)
                vm.parentMenu = false
                vm.exited = true
            }
        }
    }

    private func exitedView(btnW: CGFloat) -> some View {
        VStack(spacing: 24) {
            Text("Viszlát! 👋").font(kidFont(30)).foregroundStyle(Kid.foreground)
            KidButton(text: "Újra indítás", bg: Kid.primary, fg: Kid.background, width: btnW) {
                vm.exited = false
                setGuidedAccess(true)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

// ---- Részek ----
private struct KidBackground: View {
    var body: some View {
        GeometryReader { g in
            let r = hypot(g.size.width * 0.5, max(g.size.height * 0.35, g.size.height * 0.65))
            RadialGradient(
                gradient: Gradient(stops: [
                    .init(color: Kid.backgroundCenter, location: 0),
                    .init(color: Kid.background, location: 0.7),
                ]),
                center: UnitPoint(x: 0.5, y: 0.35),
                startRadius: 0,
                endRadius: r
            )
        }
    }
}

private struct KidButton: View {
    let text: String
    let bg: Color
    let fg: Color
    let width: CGFloat
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(text)
                .font(kidFont(21.6))
                .foregroundStyle(fg)
                .multilineTextAlignment(.center)
                .frame(width: width - 32)
                .padding(16)
                .background(bg, in: RoundedRectangle(cornerRadius: 20))
        }
        .buttonStyle(.plain)
    }
}

private struct KidKey: View {
    let label: String
    let color: Color
    let size: CGFloat
    let onDown: () -> Void
    @State private var pressed = false
    @State private var ripple = false

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 14.4)
                .fill(Color.black.opacity(0.35))
                .offset(y: pressed ? 1 : 4)
            RoundedRectangle(cornerRadius: 14.4)
                .fill(color)
                .overlay {
                    GeometryReader { proxy in
                        Circle()
                            .fill(
                                RadialGradient(
                                    colors: [.white.opacity(0.34), color.opacity(0.20), .clear],
                                    center: .center,
                                    startRadius: 2,
                                    endRadius: max(proxy.size.width, proxy.size.height) * 0.72
                                )
                            )
                            .frame(width: max(proxy.size.width, proxy.size.height) * 1.45,
                                   height: max(proxy.size.width, proxy.size.height) * 1.45)
                            .scaleEffect(ripple ? 1 : 0.12)
                            .opacity(ripple ? 0 : 0.9)
                            .position(x: proxy.size.width / 2, y: proxy.size.height / 2)
                            .allowsHitTesting(false)
                    }
                    .clipShape(RoundedRectangle(cornerRadius: 14.4))
                }
                .overlay(Text(label).font(kidFont(size)).foregroundStyle(Kid.background))
                .offset(y: pressed ? 3 : 0)
                .scaleEffect(pressed ? 0.94 : 1)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .contentShape(RoundedRectangle(cornerRadius: 14.4))
        .onLongPressGesture(minimumDuration: 0, maximumDistance: 60, pressing: { down in
            if down {
                ripple = false
                withAnimation(.easeOut(duration: 0.48)) { ripple = true }
                onDown()
            }
            pressed = down
        }, perform: {})
        // Rugalmas "lenyomódás": gyors, túllendülő lenyomás és visszarugózás.
        .animation(.spring(response: 0.25, dampingFraction: 0.42), value: pressed)
    }
}

// ---- Színpad ----
private struct StageView: View {
    let vm: KeypadViewModel
    let w: CGFloat
    let h: CGFloat

    var body: some View {
        let cell = min(w * 0.19, h * 0.34)
        // A sor középpontja: a szöveg-panel alatt, de a (billentyűzet miatt lecsökkent) színpadon belül marad.
        let cy = min(max(h * 0.60, 64 + 134 + cell / 2 + 6), h - cell / 2 - 8)
        ZStack {
            HistoryPanel(history: vm.history, bump: vm.bump)
                .frame(width: w)
                .padding(.top, 64)
                .padding(.horizontal, 20)
                .frame(width: w, height: h, alignment: .top)

            if !vm.flying.isEmpty {
                FlyingRow(items: vm.flying, start: vm.flyStart, flyId: vm.flyId, w: w, cell: cell, cy: cy)
                    .id(vm.flyId)
            }

            ForEach(Array(vm.pending.enumerated()), id: \.element.id) { i, t in
                TypedChar(item: t, slot: i, total: vm.pending.count, w: w, cell: cell, cy: cy)
            }

            ForEach(vm.bursts) { b in
                BurstView(burst: b, w: w, h: h, cell: cell, cy: cy)
            }
        }
        .frame(width: w, height: h)
        .allowsHitTesting(false)
    }
}

/// A már begépelt szöveg: minden betű a saját színével, és rugalmasan "megpattan", ha új betűk érkeznek.
private struct HistoryPanel: View {
    let history: String
    let bump: Int
    @State private var scale: CGFloat = 1

    var body: some View {
        if history.isEmpty {
            EmptyView()
        } else {
            let colored = history.suffix(48).reduce(Text("")) { acc, c in
                let code = Int(c.unicodeScalars.first?.value ?? 0)
                return acc + Text(String(c)).foregroundColor(kidColors[code % kidColors.count])
            }
            colored
                .font(kidFont(30))
                .tracking(2)
                .lineSpacing(4)
                .lineLimit(3)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(Color.white.opacity(0.07), in: RoundedRectangle(cornerRadius: 16))
                .scaleEffect(scale)
                .onChange(of: bump) { _, _ in
                    scale = 1.12
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.02) {
                        withAnimation(.spring(response: 0.35, dampingFraction: 0.35)) { scale = 1 }
                    }
                }
        }
    }
}

/// Szín keverése fehér/fekete felé (világos kitöltés, sötét körvonal).
private func mixColor(_ c: Color, _ to: Color, _ t: CGFloat) -> Color {
    var r1: CGFloat = 0, g1: CGFloat = 0, b1: CGFloat = 0, a1: CGFloat = 0
    var r2: CGFloat = 0, g2: CGFloat = 0, b2: CGFloat = 0, a2: CGFloat = 0
    UIColor(c).getRed(&r1, green: &g1, blue: &b1, alpha: &a1)
    UIColor(to).getRed(&r2, green: &g2, blue: &b2, alpha: &a2)
    return Color(red: r1 + (r2 - r1) * t, green: g1 + (g2 - g1) * t, blue: b1 + (b2 - b1) * t)
}

/// Meseszerű betű: sötét körvonal + színátmenetes kitöltés, mögötte puha kör fény.
private struct BigChar: View {
    let ch: String
    let colorIdx: Int
    let cell: CGFloat

    var body: some View {
        let c = kidColors[colorIdx]
        let light = mixColor(c, .white, 0.5)
        let deep = mixColor(c, .black, 0.38)
        let o = cell * 0.045
        let story = !ch.isEmpty && ch.allSatisfy { $0.isLetter || $0.isNumber }
        let glyph = Text(ch).font(kidFont(cell * 0.9)).lineLimit(1).fixedSize()
        ZStack {
            // Átlátszó háttér + puha, fátyolos kör fény, ami a szélén teljesen átlátszóra fakul (nincs éle, nincs elvágva).
            Circle()
                .fill(RadialGradient(
                    stops: [
                        .init(color: c.opacity(0.45), location: 0),
                        .init(color: c.opacity(0.22), location: 0.45),
                        .init(color: c.opacity(0.07), location: 0.75),
                        .init(color: c.opacity(0), location: 1),
                    ],
                    center: .center, startRadius: 0, endRadius: cell * 0.78))
                .frame(width: cell * 1.56, height: cell * 1.56)
                .allowsHitTesting(false)
            if story {
                ForEach(0..<8, id: \.self) { i in
                    let a = Double(i) * Double.pi / 4
                    glyph.foregroundStyle(deep).offset(x: CGFloat(cos(a)) * o, y: CGFloat(sin(a)) * o)
                }
                glyph.foregroundStyle(
                    LinearGradient(colors: [light, c, mixColor(c, deep, 0.45)], startPoint: .top, endPoint: .bottom)
                )
            } else {
                glyph.foregroundStyle(c)
            }
        }
        .frame(width: cell, height: cell)
    }
}

/// Egy felugró karakter: rugós (túllendülő) belépés, utána finom lebegés; a sor többi tagja rugalmasan odébb csúszik.
private struct TypedChar: View {
    let item: KidTyped
    let slot: Int
    let total: Int
    let w: CGFloat
    let cell: CGFloat
    let cy: CGFloat
    @State private var shown = false
    @State private var bobbing = false

    var body: some View {
        let dir: Double = item.id % 2 == 0 ? -1 : 1
        let targetX = (w - CGFloat(total) * cell) / 2 + CGFloat(slot) * cell + cell / 2
        BigChar(ch: item.ch, colorIdx: item.colorIdx, cell: cell)
            .scaleEffect(x: shown ? 1 : 0, y: shown ? 1 : 0)
            .rotationEffect(.degrees(shown ? 0 : 28 * dir))
            .offset(y: shown ? 0 : -cell * 0.5)
            .offset(y: bobbing ? -4 : 4)
            .animation(.easeInOut(duration: 1.2).delay(Double(slot) * 0.2).repeatForever(autoreverses: true), value: bobbing)
            .position(x: targetX, y: cy)
            .animation(.spring(response: 0.45, dampingFraction: 0.55), value: targetX)
            .onAppear {
                withAnimation(.spring(response: 0.5, dampingFraction: 0.34)) { shown = true }
                bobbing = true
            }
    }
}

/// Az 5 betű hullámszerűen felrepül a begépelt szövegbe (kis "lendület-vétellel"), közben kicsinyedve elhalványul.
private struct FlyingRow: View {
    let items: [KidTyped]
    let start: Date
    let flyId: Int
    let w: CGFloat
    let cell: CGFloat
    let cy: CGFloat
    @State private var done = false

    var body: some View {
        TimelineView(.animation(paused: done)) { ctx in
            let p = ctx.date.timeIntervalSince(start) * 1000 // ms
            ZStack {
                ForEach(Array(items.enumerated()), id: \.element.id) { i, t in
                    let local = min(1, max(0, (p - Double(i) * 45) / 460))
                    let e = launchEase.value(local)
                    let x = (w - CGFloat(items.count) * cell) / 2 + CGFloat(i) * cell + cell / 2
                    let sc = CGFloat(1 - 0.7 * e)
                    BigChar(ch: t.ch, colorIdx: t.colorIdx, cell: cell)
                        .scaleEffect(sc)
                        .rotationEffect(.degrees(e * Double(i - 2) * 18))
                        .opacity(local > 0.7 ? 1 - (local - 0.7) / 0.3 : 1)
                        .position(x: x, y: cy - CGFloat(e) * (cy - 104))
                }
            }
        }
        .task(id: flyId) {
            done = false
            try? await Task.sleep(nanoseconds: 750_000_000)
            done = true
        }
    }
}

// ---- Mókás konfetti ----
private struct Particle {
    let vx: Double, vy: Double, spin: Double
    let kind: Int
    let color: Color
    let size: Double
    let phase: Double
}

private func makeParticles(_ b: KidBurst) -> [Particle] {
    let n = b.kind == 0 ? 22 : (b.kind == 1 ? 46 : 40)
    return (0..<n).map { i in
        let ang = b.kind == 0 ? (-90.0 + (Double.random(in: 0..<1) - 0.5) * 140.0) : Double.random(in: 0..<360)
        let speed = b.kind == 0 ? 280 + Double.random(in: 0..<420) : 300 + Double.random(in: 0..<500)
        let rad = ang * Double.pi / 180
        var vy = sin(rad) * speed
        if b.kind != 0 { vy -= 220 }
        let kind: Int
        switch Int.random(in: 0..<10) {
        case 0, 1, 2: kind = 0
        case 3, 4: kind = 1
        case 5, 6: kind = 2
        case 7: kind = 3
        case 8: kind = 4
        default: kind = 5
        }
        return Particle(
            vx: cos(rad) * speed, vy: vy,
            spin: (Double.random(in: 0..<1) - 0.5) * 2 * (360 + Double.random(in: 0..<720)),
            kind: kind, color: kidColors[(i + b.seed) % kidColors.count],
            size: 8 + Double.random(in: 0..<10), phase: Double.random(in: 0..<6.28)
        )
    }
}

private struct BurstView: View {
    let burst: KidBurst
    let w: CGFloat
    let h: CGFloat
    let cell: CGFloat
    let cy: CGFloat
    @State private var parts: [Particle]
    @State private var done = false

    init(burst: KidBurst, w: CGFloat, h: CGFloat, cell: CGFloat, cy: CGFloat) {
        self.burst = burst
        self.w = w
        self.h = h
        self.cell = cell
        self.cy = cy
        _parts = State(initialValue: makeParticles(burst))
    }

    var body: some View {
        TimelineView(.animation(paused: done)) { ctx in
            Canvas { context, size in
                let s = ctx.date.timeIntervalSince(burst.start)
                let ox = burst.kind == 2
                    ? size.width / 2
                    : (size.width - CGFloat(burst.total) * cell) / 2 + (CGFloat(burst.slot) + 0.5) * cell
                let oy = cy
                for q in parts { draw(q, s, ox, oy, &context) }
            }
        }
        .frame(width: w, height: h)
        .allowsHitTesting(false)
        .task {
            try? await Task.sleep(nanoseconds: 1_900_000_000)
            done = true
        }
    }

    private func draw(_ q: Particle, _ s: Double, _ ox: CGFloat, _ oy: CGFloat, _ context: inout GraphicsContext) {
        let life = 1.7
        if s < 0 || s > life { return }
        let k = 2.2
        let d = (1 - exp(-k * s)) / k
        let x = Double(ox) + q.vx * d + sin(q.phase + s * 7) * 6 * min(s, 1)
        let y = Double(oy) + q.vy * d + 0.5 * 900 * s * s
        let a = s > life * 0.65 ? 1 - (s - life * 0.65) / (life * 0.35) : 1
        let sc = s < 0.12 ? s / 0.12 * 1.25 : (s < 0.2 ? 1.25 - (s - 0.12) / 0.08 * 0.25 : 1)
        var c = context
        c.translateBy(x: x, y: y)
        c.rotate(by: .degrees(q.spin * s))
        c.scaleBy(x: sc, y: sc)
        c.opacity = min(1, max(0, a))
        let sz = q.size
        switch q.kind {
        case 0:
            c.fill(Path(roundedRect: CGRect(x: -sz * 0.5, y: -sz * 0.75, width: sz, height: sz * 1.5), cornerRadius: sz * 0.2),
                   with: .color(q.color))
        case 1:
            c.fill(Path(ellipseIn: CGRect(x: -sz * 0.55, y: -sz * 0.55, width: sz * 1.1, height: sz * 1.1)),
                   with: .color(q.color))
        case 2:
            var star = Path()
            for i in 0..<10 {
                let r = i % 2 == 0 ? sz : sz * 0.45
                let ang = (-90.0 + Double(i) * 36) * Double.pi / 180
                let p = CGPoint(x: cos(ang) * r, y: sin(ang) * r)
                if i == 0 { star.move(to: p) } else { star.addLine(to: p) }
            }
            star.closeSubpath()
            c.fill(star, with: .color(q.color))
        case 3:
            var heart = Path()
            heart.move(to: CGPoint(x: 0, y: sz * 0.9))
            heart.addCurve(to: CGPoint(x: 0, y: -sz * 0.4),
                           control1: CGPoint(x: -sz * 1.4, y: -sz * 0.1),
                           control2: CGPoint(x: -sz * 0.7, y: -sz * 1.1))
            heart.addCurve(to: CGPoint(x: 0, y: sz * 0.9),
                           control1: CGPoint(x: sz * 0.7, y: -sz * 1.1),
                           control2: CGPoint(x: sz * 1.4, y: -sz * 0.1))
            heart.closeSubpath()
            c.fill(heart, with: .color(q.color))
        case 4:
            var arc = Path()
            arc.addArc(center: .zero, radius: sz, startAngle: .degrees(135), endAngle: .degrees(315), clockwise: false)
            c.stroke(arc, with: .color(q.color), lineWidth: sz * 0.3)
        default:
            c.fill(Path(roundedRect: CGRect(x: -sz * 0.18, y: -sz * 1.5, width: sz * 0.36, height: sz * 3), cornerRadius: sz * 0.15),
                   with: .color(q.color))
        }
    }
}
