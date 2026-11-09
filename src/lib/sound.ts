let ctx: AudioContext | null = null;
function ac() {
  if (!ctx) ctx = new (window.AudioContext || (window as any).webkitAudioContext)();
  if (ctx.state === "suspended") ctx.resume();
  return ctx;
}

// Pentatonic xylophone notes (C major pentatonic)
const NOTES = [523.25, 587.33, 659.25, 783.99, 880, 1046.5, 1174.66, 1318.51];

export function playXylo(seed: number) {
  const a = ac();
  const f = NOTES[Math.abs(seed) % NOTES.length] ?? 523.25;
  const t = a.currentTime;
  const g = a.createGain();
  g.gain.setValueAtTime(0.0001, t);
  g.gain.exponentialRampToValueAtTime(0.25, t + 0.01);
  g.gain.exponentialRampToValueAtTime(0.0001, t + 0.9);
  g.connect(a.destination);
  [1, 4].forEach((m, i) => {
    const o = a.createOscillator();
    o.type = "sine";
    o.frequency.value = f * m;
    const og = a.createGain();
    og.gain.value = i === 0 ? 1 : 0.15;
    o.connect(og).connect(g);
    o.start(t);
    o.stop(t + 1);
  });
}

export function playPop() {
  const a = ac();
  const t = a.currentTime;
  const o = a.createOscillator();
  const g = a.createGain();
  o.type = "sine";
  o.frequency.setValueAtTime(900, t);
  o.frequency.exponentialRampToValueAtTime(180, t + 0.12);
  g.gain.setValueAtTime(0.2, t);
  g.gain.exponentialRampToValueAtTime(0.0001, t + 0.15);
  o.connect(g).connect(a.destination);
  o.start(t);
  o.stop(t + 0.2);
}
