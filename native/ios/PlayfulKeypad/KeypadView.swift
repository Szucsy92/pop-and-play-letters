import SwiftUI
import UIKit

private let kidColors: [Color] = [.pink,.orange,.mint,.blue,.purple,.yellow]
private let keypadRows: [[String]] = [Array("1234567890").map(String.init), Array("QWERTZUIOP").map(String.init), Array("ASDFGHJKLÉ").map(String.init), Array("YXCVBNMÖÜÁ").map(String.init), ["?","!","+","-","★","♥","☺","."]]

struct KeypadView: View {
    @State private var vm = KeypadViewModel()
    @State private var systemInput = ""
    @State private var guidedAccess = UIAccessibility.isGuidedAccessEnabled
    @FocusState private var keyboardFocused: Bool

    var body: some View {
        ZStack {
            Color(red: 0.09, green: 0.10, blue: 0.14).ignoresSafeArea()
            if vm.exited {
                VStack(spacing: 24) {
                    Text("Viszlát! 👋").font(.largeTitle.bold()).foregroundStyle(.white)
                    Button("Újra indítás") { vm.exited=false }
                }
            } else {
                VStack(spacing: 0) {
                    ZStack {
                        ConfettiCanvas(seed: vm.characterID).allowsHitTesting(false)
                        if !vm.character.isEmpty {
                            Text(vm.character).font(.system(size: 140, weight: .black, design: .rounded))
                                .foregroundStyle(kidColors[Int((vm.character.unicodeScalars.first?.value ?? 0))%kidColors.count])
                                .id(vm.characterID)
                                .transition(.scale.combined(with: .opacity))
                                .animation(.spring(response: 0.35, dampingFraction: 0.5), value: vm.characterID)
                        }
                    }.frame(maxWidth: .infinity).frame(maxHeight: vm.mode == "inner" ? .infinity : .infinity)
                    if vm.mode == "inner" {
                        VStack(spacing: 6) {
                            ForEach(keypadRows.indices, id: \.self) { row in
                                HStack(spacing: 5) {
                                    ForEach(keypadRows[row].indices, id: \.self) { col in
                                        let key=keypadRows[row][col]
                                        Button { vm.press(key) } label: {
                                            Text(key).font(.system(size: row == 0 ? 18 : 21, weight: .bold))
                                                .frame(maxWidth: .infinity, maxHeight: .infinity)
                                                .background(kidColors[(row*3+col)%kidColors.count], in: RoundedRectangle(cornerRadius: 12))
                                                .foregroundStyle(Color.black.opacity(0.75))
                                        }.buttonStyle(.plain)
                                    }
                                }.frame(maxHeight: .infinity)
                            }
                        }.frame(height: UIScreen.main.bounds.height * 0.43).padding(8)
                    } else {
                        TextField("", text: $systemInput)
                            .textInputAutocapitalization(.characters).autocorrectionDisabled()
                            .focused($keyboardFocused).frame(width: 1, height: 1).opacity(0.01)
                            .onChange(of: systemInput) { _, value in
                                if let ch=value.last { vm.press(String(ch)); systemInput="" }
                            }
                            .onAppear { keyboardFocused=true }
                    }
                }
                VStack { HStack { Spacer(); Button { vm.registerLockTap() } label: { Text("🔒").font(.title).opacity(0.55).padding() } }; Spacer() }
            }
            if vm.parentMenu {
                Color.black.opacity(0.94).ignoresSafeArea()
                VStack(spacing: 18) {
                    Text("Szülői menü").font(.largeTitle.bold()).foregroundStyle(.white)
                    Button("Billentyűzet: \(vm.mode == "inner" ? "nagy gombok" : "telefoné") ↔") { vm.mode = vm.mode == "inner" ? "native" : "inner" }
                    Button("Hang: \(vm.soundEnabled ? "be 🔊" : "ki 🔇")") { vm.soundEnabled.toggle() }
                    Button(guidedAccess ? "Irányított hozzáférés aktív ✓" : "Irányított hozzáférés bekapcsolása") {
                        guidedAccess = UIAccessibility.isGuidedAccessEnabled
                        if !guidedAccess { UIApplication.shared.open(URL(string: "App-prefs:ACCESSIBILITY")!) }
                    }
                    Text("iOS-en a Guided Access-t a szülő kapcsolja be; az app nem aktiválhatja programból.").font(.footnote).foregroundStyle(.white.opacity(0.7)).multilineTextAlignment(.center)
                    Button("Vissza a játékhoz") { vm.parentMenu=false; if vm.mode=="native" { keyboardFocused=true } }
                    Button("Kilépés") { vm.exited=true; vm.parentMenu=false }.foregroundStyle(.red)
                }.padding(24)
            }
        }
        .preferredColorScheme(.dark)
        .statusBarHidden(true)
    }
}

private struct ConfettiCanvas: View {
    let seed: Int
    var body: some View {
        Canvas { context, size in
            guard seed > 0 else { return }
            for i in 0..<28 {
                let x = CGFloat((seed*31+i*73)%1000)/1000 * size.width
                let y = CGFloat((seed*17+i*47)%1000)/1000 * size.height
                let rect=CGRect(x:x,y:y,width:5,height:5)
                context.fill(Path(ellipseIn: rect), with: .color(kidColors[i%kidColors.count].opacity(0.8)))
            }
        }
    }
}
