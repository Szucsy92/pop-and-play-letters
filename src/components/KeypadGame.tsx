import { useCallback, useEffect, useRef, useState } from "react";
import { playPop, playXylo, unlockAudio } from "@/lib/sound";
import { MobileRelease, exitNative } from "@/components/MobileRelease";
type Mode = "inner" | "native";
type Shown = { id: number; ch: string; color: string; anim: number };
type Bit = { id: number; x: number; y: number; dx: number; dy: number; r: number; color: string; kind: number };

const COLORS: string[] = ["var(--kid-1)", "var(--kid-2)", "var(--kid-3)", "var(--kid-4)", "var(--kid-5)", "var(--kid-6)"];
const ROWS = [
  "1234567890".split(""),
  "QWERTZUIOP".split(""),
  "ASDFGHJKLÉ".split(""),
  "YXCVBNMÖÜÁ".split(""),
  ["?", "!", "+", "-", "★", "♥", "☺", "."],
];
let uid = 0;

export function KeypadGame() {
  const [mode, setMode] = useState<Mode | null>(null);
  const [shown, setShown] = useState<Shown | null>(null);
  const [bits, setBits] = useState<Bit[]>([]);
  const [menu, setMenu] = useState(false);
  const [sound, setSound] = useState(true);
  const [exited, setExited] = useState(false);
  const [kbInset, setKbInset] = useState(0);
  const taps = useRef<number[]>([]);
  const inputRef = useRef<HTMLInputElement>(null);
  const menuRef = useRef(false);
  menuRef.current = menu;

  useEffect(() => {
    const m = localStorage.getItem("kidkb-mode") as Mode | null;
    if (m) setMode(m);
  }, []);

  // Telefon saját billentyűzete: a betű a látható terület közepére kerüljön, ne a billentyűzet mögé.
  // Ha az ablak magától átméreteződik (adjustResize), az eltolás 0 marad.
  useEffect(() => {
    const vv = window.visualViewport;
    if (mode !== "native" || menu || !vv) { setKbInset(0); return; }
    const update = () => {
      const hidden = Math.round(window.innerHeight - (vv.height + vv.offsetTop));
      setKbInset(hidden > 80 ? hidden : 0);
    };
    update();
    vv.addEventListener("resize", update);
    vv.addEventListener("scroll", update);
    return () => { vv.removeEventListener("resize", update); vv.removeEventListener("scroll", update); };
  }, [mode, menu]);

  // Block gestures, context menu, back navigation
  useEffect(() => {
    if (exited) return;
    const prevent = (e: Event) => e.preventDefault();
    document.addEventListener("touchmove", prevent, { passive: false });
    document.addEventListener("contextmenu", prevent);
    document.addEventListener("gesturestart", prevent);
    history.pushState(null, "", location.href);
    const onPop = () => history.pushState(null, "", location.href);
    window.addEventListener("popstate", onPop);
    const onUnload = (e: BeforeUnloadEvent) => { e.preventDefault(); };
    window.addEventListener("beforeunload", onUnload);
    return () => {
      document.removeEventListener("touchmove", prevent);
      document.removeEventListener("contextmenu", prevent);
      document.removeEventListener("gesturestart", prevent);
      window.removeEventListener("popstate", onPop);
      window.removeEventListener("beforeunload", onUnload);
    };
  }, [exited]);

  const goFull = () => {
    const el: any = document.documentElement;
    if (!document.fullscreenElement) el.requestFullscreen?.({ navigationUI: "hide" }).catch(() => {});
    (screen.orientation as any)?.lock?.("portrait").catch?.(() => {});
  };

  const press = useCallback((ch: string) => {
    if (!ch || ch === " ") return;
    const seed = ch.charCodeAt(0);
    if (sound) (seed % 3 === 0 ? playPop() : playXylo(seed));
    setShown({ id: ++uid, ch: ch.toUpperCase(), color: COLORS[seed % COLORS.length] ?? COLORS[0] ?? "var(--kid-1)", anim: seed % 4 });
    const n: Bit[] = Array.from({ length: 22 }, (_, i) => {
      const ang = Math.random() * Math.PI * 2;
      const dist = 120 + Math.random() * 260;
      return { id: ++uid, x: 50, y: 40, dx: Math.cos(ang) * dist, dy: Math.sin(ang) * dist, r: Math.random() * 720 - 360, color: COLORS[(i + seed) % COLORS.length] ?? "var(--kid-1)", kind: i % 5 === 0 ? 1 : 0 };
    });
    setBits(n);
  }, [sound]);

  useEffect(() => {
    if (mode !== "native" || menu) return;
    const onKey = (e: KeyboardEvent) => { if (e.key.length === 1) press(e.key); };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [mode, menu, press]);

  const choose = (m: Mode) => {
    localStorage.setItem("kidkb-mode", m);
    setMode(m);
    unlockAudio();
    goFull();
    if (m === "native") setTimeout(() => inputRef.current?.focus(), 50);
  };

  const lockTap = () => {
    const now = Date.now();
    taps.current = [...taps.current.filter((t) => now - t < 2000), now];
    if (taps.current.length >= 5) { taps.current = []; setMenu(true); inputRef.current?.blur(); }
  };

  const closeMenu = () => { setMenu(false); goFull(); if (mode === "native") setTimeout(() => inputRef.current?.focus(), 50); };

  if (exited) {
    return (
      <div className="kid-root flex flex-col items-center justify-center gap-6 text-center">
        <p className="text-3xl font-bold">Viszlát! 👋</p>
        <button className="kid-btn bg-primary text-primary-foreground" onClick={() => { setExited(false); goFull(); }}>Újra indítás</button>
      </div>
    );
  }

  return (
    <div className="kid-root" onClick={() => { if (mode === "native" && !menu) inputRef.current?.focus(); }}>
      {/* lock */}
      <button aria-label="Lakat" onPointerDown={(e) => { e.stopPropagation(); lockTap(); }} className="absolute right-3 top-3 z-30 grid h-12 w-12 place-items-center rounded-full text-2xl opacity-40">🔒</button>

      {/* stage */}
      <div className={`absolute inset-x-0 top-0 flex items-center justify-center ${mode === "inner" ? "bottom-[46%]" : "bottom-0"}`} style={mode === "native" && kbInset ? { bottom: kbInset } : undefined}>
        {bits.map((b) => (
          <span key={b.id} className={b.kind ? "kid-curl" : "kid-confetti"} style={{ ["--dx" as any]: `${b.dx}px`, ["--dy" as any]: `${b.dy}px`, ["--r" as any]: `${b.r}deg`, color: b.color, background: b.kind ? "transparent" : b.color }} />
        ))}
        {shown && (
          <span key={shown.id} className={`kid-char kid-anim-${shown.anim}`} style={{ color: shown.color }}>{shown.ch}</span>
        )}
      </div>

      {mode === "native" && (
        <input ref={inputRef} className="fixed bottom-0 left-0 h-px w-px opacity-0" inputMode="text" enterKeyHint="done" autoCapitalize="characters" autoCorrect="off" autoComplete="off" spellCheck={false} aria-label="Billentyűzet"
          onBlur={() => { if (!menuRef.current) setTimeout(() => { if (!menuRef.current) inputRef.current?.focus(); }, 120); }}
          onInput={(e) => { const v = e.currentTarget.value; if (v) press(v.slice(-1)); e.currentTarget.value = ""; }} />
      )}

      {mode === "inner" && (
        <div className="absolute inset-x-0 bottom-0 flex h-[46%] flex-col gap-1.5 p-2 pb-4">
          {ROWS.map((row, ri) => (
            <div key={ri} className="flex flex-1 gap-1.5">
              {row.map((k, ki) => (
                <button key={k} onPointerDown={(e) => { e.preventDefault(); press(k); }}
                  className="kid-key flex-1" style={{ background: COLORS[(ri * 3 + ki) % COLORS.length] }}>{k}</button>
              ))}
            </div>
          ))}
        </div>
      )}

      {mode === null && (
        <div className="absolute inset-0 z-40 flex flex-col items-center justify-center gap-5 bg-background/95 p-6 text-center">
          <p className="text-3xl font-bold">Melyik billentyűzet legyen?</p>
          <button className="kid-btn bg-primary text-primary-foreground" onClick={() => choose("inner")}>🎨 Nagy színes gombok</button>
          <button className="kid-btn bg-secondary text-secondary-foreground" onClick={() => choose("native")}>📱 Telefon billentyűzete</button>
          <p className="max-w-xs text-sm text-muted-foreground">Kilépés és beállítások: 5× gyors koppintás a jobb felső lakatra.</p>
        </div>
      )}

      {menu && (
        <div className="absolute inset-0 z-50 flex flex-col items-center justify-center gap-4 bg-background/95 p-6">
          <p className="mb-2 text-3xl font-bold">Szülői menü</p>
          <button className="kid-btn bg-secondary text-secondary-foreground" onClick={() => { const m = mode === "inner" ? "native" : "inner"; localStorage.setItem("kidkb-mode", m); setMode(m); }}>
            Billentyűzet: {mode === "inner" ? "nagy gombok" : "telefoné"} ↔
          </button>
          <button className="kid-btn bg-secondary text-secondary-foreground" onClick={() => setSound(!sound)}>Hang: {sound ? "be 🔊" : "ki 🔇"}</button>
          <button className="kid-btn bg-primary text-primary-foreground" onClick={closeMenu}>Vissza a játékhoz</button>
          <MobileRelease />
          <button className="kid-btn bg-destructive text-destructive-foreground" onClick={() => { setMenu(false); setExited(true); void exitNative(); document.exitFullscreen?.().catch(() => {}); }}>Kilépés</button>
        </div>
      )}
    </div>
  );
}
