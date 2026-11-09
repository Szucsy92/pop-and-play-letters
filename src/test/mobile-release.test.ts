import { describe, expect, it } from 'vitest';
import { parseRelease } from '@/lib/mobile-release';
const row = { versionCode: 2, version: '2026.11.09-0834', apkUrl: 'https://github.com/example/keypad/releases/download/build-2/playful-keypad-2026-11-09_0834.apk' };
describe('Mobilos kiadás', () => {
  it('csak újabb APK-t kínál fel', () => {
    expect(parseRelease(row, 'example/keypad', 1)?.versionCode).toBe(2);
    expect(parseRelease(row, 'example/keypad', 2)).toBeNull();
    expect(parseRelease(row, 'example/keypad', 3)).toBeNull();
  });
  it('megőrzi a dátumos verziójelzést', () => { expect(parseRelease(row, 'example/keypad', 1)?.version).toBe('2026.11.09-0834'); });
  it('elutasít más helyről származó telepítőt', () => {
    expect(() => parseRelease({ ...row, apkUrl: 'https://evil.example/app.apk' }, 'example/keypad', 1)).toThrow();
    expect(() => parseRelease(row, 'other/app', 1)).toThrow();
  });
});
