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

// ---- Animációs segédek (CSS keyframes portja) ----
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

private let kidEase = CubicBezier(x1: 0.2, y1: 0.9, x2: 0.3, y2: 1.2)
private let flyEase = CubicBezier(x1: 0, y1: 0, x2: 0.58, y2: 1)

private func track(_ t: Double, _ stops: [(Double, Double)]) -> Double {
    guard let first = stops.first, let last = stops.last else { return 0 }
    if t <= first.0 { return first.1 }
    for i in 1..<stops.count {
        let (t1, v1) = stops[i]
        if t <= t1 {
            let (t0, v0) = stops[i - 1]
            let local = (t - t0) / (t1 - t0)
            return v0 + (v1 - v0) * kidEase.value(local)
        }
    }
    return last.1
}

private struct CharFrame {
    var scale = 1.0
    var scaleY = 1.0
    var rot = 0.0
    var ty = 0.0
    var alpha = 1.0
}

private func charFrame(anim: Int, t: Double, size: Double, screenH: Double) -> CharFrame {
    var f = CharFrame()
    switch anim {
    case 0:
        f.scale = track(t, [(0, 0), (0.2, 1.15), (0.35, 1), (1, 0.6)])
        f.alpha = track(t, [(0, 0), (0.2, 1), (0.75, 1), (1, 0)])
    case 1:
        f.scale = track(t, [(0, 0), (0.2, 1), (0.75, 1), (1, 0.7)])
        f.rot = track(t, [(0, 0), (0.2, -10), (0.35, 8), (0.5, -4), (0.75, 0), (1, 0)])
        f.alpha = track(t, [(0, 1), (0.75, 1), (1, 0)])
    case 2:
        let fall = track(t, [(0, -0.6 * screenH), (0.25, 0)])
        let bounce = track(t, [(0.25, 0), (0.35, -0.08), (0.45, 0), (1, 0.10)]) * size
        f.ty = fall + bounce
        f.scaleY = track(t, [(0, 1), (0.25, 0.85), (0.35, 1), (1, 1)])
        f.alpha = track(t, [(0, 1), (0.75, 1), (1, 0)])
    default:
        f.rot = track(t, [(0, -200), (0.3, 0), (1, 0)])
        f.scale = track(t, [(0, 0), (0.3, 1), (1, 1.3)])
        f.alpha = track(t, [(0, 1), (0.75, 1), (1, 0)])
    }
    f.alpha = min(1, max(0, f.alpha))
    return f
}

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
            // színpad
            ZStack {
                if let shown = vm.shown {
                    BitsView(bits: vm.bits, start: shown.start, token: shown.id)
                    StageChar(shown: shown, size: min(w * 0.55, h * 0.38), screenH: h)
                }
            }
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
                    .frame(width: w, height: h * 0.46)
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
        return VStack(spacing: 6) {
            ForEach(0..<keypadRows.count, id: \.self) { ri in
                HStack(spacing: 6) {
                    ForEach(0..<keypadRows[ri].count, id: \.self) { ki in
                        let key = keypadRows[ri][ki]
                        KidKey(label: key, color: kidColors[(ri * 3 + ki) % kidColors.count], size: fontSize) {
                            vm.press(key)
                        }
                    }
                }
                .frame(maxHeight: .infinity)
            }
        }
        .padding(EdgeInsets(top: 8, leading: 8, bottom: 16, trailing: 8))
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

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 14.4)
                .fill(Color.black.opacity(0.35))
                .offset(y: pressed ? 1 : 4)
            RoundedRectangle(cornerRadius: 14.4)
                .fill(color)
                .overlay(Text(label).font(kidFont(size)).foregroundStyle(Kid.background))
                .offset(y: pressed ? 3 : 0)
                .scaleEffect(pressed ? 0.96 : 1)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .contentShape(Rectangle())
        .onLongPressGesture(minimumDuration: 0, maximumDistance: 60, pressing: { down in
            pressed = down
            if down { onDown() }
        }, perform: {})
        .animation(.easeOut(duration: 0.08), value: pressed)
    }
}

private struct StageChar: View {
    let shown: KidShown
    let size: CGFloat
    let screenH: CGFloat
    @State private var done = false

    var body: some View {
        TimelineView(.animation(paused: done)) { ctx in
            let t = min(1, max(0, ctx.date.timeIntervalSince(shown.start) / 1.6))
            let f = charFrame(anim: shown.anim, t: t, size: Double(size), screenH: Double(screenH))
            Text(shown.ch)
                .font(kidFont(size))
                .foregroundStyle(kidColors[shown.colorIdx])
                .shadow(color: Color.black.opacity(0.35), radius: 0, x: 0, y: size * 0.06)
                .lineLimit(1)
                .fixedSize()
                .scaleEffect(x: CGFloat(f.scale), y: CGFloat(f.scale * f.scaleY))
                .rotationEffect(.degrees(f.rot))
                .offset(y: CGFloat(f.ty))
                .opacity(f.alpha)
        }
        .task(id: shown.id) {
            done = false
            try? await Task.sleep(nanoseconds: 1_700_000_000)
            done = true
        }
    }
}

private struct BitsView: View {
    let bits: [KidBit]
    let start: Date
    let token: Int
    @State private var done = false

    var body: some View {
        TimelineView(.animation(paused: done)) { ctx in
            Canvas { context, size in
                let raw = min(1, max(0, ctx.date.timeIntervalSince(start) / 1.4))
                let e = flyEase.value(raw)
                let cx = size.width / 2
                let cy = size.height / 2
                for b in bits {
                    var c = context
                    c.translateBy(x: cx + b.dx * e, y: cy + (b.dy + 80) * e)
                    c.rotate(by: .degrees(b.r * e))
                    c.opacity = 1 - e
                    let color = kidColors[b.colorIdx]
                    if b.curl {
                        var path = Path()
                        path.addArc(center: .zero, radius: 17.5, startAngle: .degrees(135),
                                    endAngle: .degrees(315), clockwise: false)
                        c.stroke(path, with: .color(color), lineWidth: 5)
                    } else {
                        c.fill(Path(roundedRect: CGRect(x: -6, y: -9, width: 12, height: 18), cornerRadius: 3),
                               with: .color(color))
                    }
                }
            }
        }
        .allowsHitTesting(false)
        .task(id: token) {
            done = false
            try? await Task.sleep(nanoseconds: 1_500_000_000)
            done = true
        }
    }
}
