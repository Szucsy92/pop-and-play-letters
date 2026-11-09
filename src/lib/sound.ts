// Lágy, recsegésmentes hangok.
// - Minden hang közös lánczon megy át: főhangerő → aluláteresztő szűrő → kompresszor.
// - Minden hang puha felfutással indul és lecsengéssel ér véget (nincs hirtelen vágás = nincs kattogás).
// - Egyszerre legfeljebb MAX_VOICES hang szólhat; a régebbieket finoman elhalkítjuk, így gyors gépeléskor sem torlódnak.

type Voice = { duck: GainNode; stop: (when: number) => void; done: boolean };

const MAX_VOICES = 4;
const MIN_GAP = 0.03; // másodperc: ennél sűrűbb leütésnél kihagyjuk a hangot
const NOTES = [523.25, 587.33, 659.25, 783.99, 880, 1046.5, 1174.66, 1318.51]; // C-dúr pentaton

let ctx: AudioContext | null = null;
let bus: AudioNode | null = null;
let voices: Voice[] = [];
let lastAt = -1;

function init(): AudioContext | null {
  if (ctx) return ctx;
  const AC: typeof AudioContext | undefined = window.AudioContext || (window as any).webkitAudioContext;
  if (!AC) return null;
  ctx = new AC();
  const master = ctx.createGain();
  master.gain.value = 0.55;
  const lp = ctx.createBiquadFilter();
  lp.type = "lowpass";
  lp.frequency.value = 4200;
  lp.Q.value = 0.5;
  const comp = ctx.createDynamicsCompressor();
  comp.threshold.value = -20;
  comp.knee.value = 24;
  comp.ratio.value = 4;
  comp.attack.value = 0.005;
  comp.release.value = 0.25;
  master.connect(lp);
  lp.connect(comp);
  comp.connect(ctx.destination);
  bus = master;
  document.addEventListener("visibilitychange", () => {
    if (!ctx) return;
    if (document.hidden) void ctx.suspend().catch(() => {});
    else void ctx.resume().catch(() => {});
  });
  return ctx;
}

/** Hívd meg egy koppintás közben, hogy az első hang is késés nélkül szóljon. */
export function unlockAudio() {
  const a = init();
  if (a && a.state !== "running") void a.resume().catch(() => {});
}

function ready(): AudioContext | null {
  const a = init();
  if (!a || !bus) return null;
  if (a.state !== "running") void a.resume().catch(() => {});
  const now = a.currentTime;
  if (now - lastAt < MIN_GAP) return null;
  lastAt = now;
  return a;
}

function register(a: AudioContext, voice: Voice) {
  const now = a.currentTime;
  // a korábbi hangokat halkítjuk, hogy az új tisztán kiváljon
  for (const v of voices) v.duck.gain.setTargetAtTime(0.4, now, 0.04);
  voices.push(voice);
  while (voices.length > MAX_VOICES) {
    const old = voices.shift();
    if (!old) break;
    old.duck.gain.setTargetAtTime(0, now, 0.025);
    old.stop(now + 0.25);
  }
}

function finish(voice: Voice, nodes: AudioNode[]) {
  voice.done = true;
  voices = voices.filter((v) => v !== voice);
  for (const n of nodes) {
    try { n.disconnect(); } catch { /* már lekapcsolva */ }
  }
}

export function playXylo(seed: number) {
  const a = ready();
  if (!a || !bus) return;
  const idx = Math.abs(seed) % NOTES.length;
  const f = NOTES[idx] ?? 523.25;
  const t = a.currentTime + 0.005;
  const peak = 0.3 - idx * 0.012; // a magasabb hangok kicsit halkabbak

  const env = a.createGain();
  const duck = a.createGain();
  env.gain.setValueAtTime(0, t);
  env.gain.linearRampToValueAtTime(peak, t + 0.012);
  env.gain.setTargetAtTime(0, t + 0.012, 0.2);
  env.connect(duck).connect(bus);

  const main = a.createOscillator();
  main.type = "sine";
  main.frequency.value = f;
  main.connect(env);

  // halk, gyorsan elhaló felhang a xilofonos "kopogáshoz" (csak mélyebb hangoknál)
  const nodes: AudioNode[] = [main, env, duck];
  const oscs: OscillatorNode[] = [main];
  if (f < 900) {
    const ov = a.createOscillator();
    const og = a.createGain();
    ov.type = "sine";
    ov.frequency.value = f * 3.99;
    og.gain.setValueAtTime(0, t);
    og.gain.linearRampToValueAtTime(0.1, t + 0.006);
    og.gain.setTargetAtTime(0, t + 0.006, 0.04);
    ov.connect(og).connect(env);
    oscs.push(ov);
    nodes.push(ov, og);
  }

  const end = t + 1.8;
  const voice: Voice = {
    duck,
    done: false,
    stop: (when) => oscs.forEach((o) => { try { o.stop(when); } catch { /* már leállt */ } }),
  };
  main.onended = () => finish(voice, nodes);
  oscs.forEach((o) => { o.start(t); o.stop(end); });
  register(a, voice);
}

export function playPop() {
  const a = ready();
  if (!a || !bus) return;
  const t = a.currentTime + 0.005;

  const o = a.createOscillator();
  const env = a.createGain();
  const duck = a.createGain();
  o.type = "sine";
  o.frequency.setValueAtTime(620, t);
  o.frequency.exponentialRampToValueAtTime(240, t + 0.1);
  env.gain.setValueAtTime(0, t);
  env.gain.linearRampToValueAtTime(0.28, t + 0.008);
  env.gain.setTargetAtTime(0, t + 0.008, 0.035);
  o.connect(env).connect(duck).connect(bus);

  const nodes: AudioNode[] = [o, env, duck];
  const voice: Voice = {
    duck,
    done: false,
    stop: (when) => { try { o.stop(when); } catch { /* már leállt */ } },
  };
  o.onended = () => finish(voice, nodes);
  o.start(t);
  o.stop(t + 0.45);
  register(a, voice);
}
