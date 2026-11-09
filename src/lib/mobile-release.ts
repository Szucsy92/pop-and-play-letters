export type Release = { versionCode: number; version: string; apkUrl: string };
export function parseRelease(data: unknown, repository: string, currentCode: number): Release | null {
  if (!data || typeof data !== 'object') throw new Error('Érvénytelen kiadás.');
  const row = data as Record<string, unknown>;
  const versionCode = row['versionCode'];
  const version = row['version'];
  const apkUrl = row['apkUrl'];
  if (typeof versionCode !== 'number' || !Number.isSafeInteger(versionCode) || versionCode < 1 || typeof version !== 'string' || !version || typeof apkUrl !== 'string') throw new Error('Hiányos verzióadatok.');
  const url = new URL(apkUrl);
  if (url.protocol !== 'https:' || url.hostname !== 'github.com' || !url.pathname.startsWith(`/${repository}/releases/download/`) || !url.pathname.endsWith('.apk') || url.username || url.password) throw new Error('Érvénytelen telepítőcím.');
  return versionCode > currentCode ? { versionCode, version, apkUrl } : null;
}
export async function checkRelease(repository: string, currentCode: number): Promise<Release | null> {
  if (!/^[\w.-]+\/[\w.-]+$/.test(repository)) throw new Error('Nincs beállítva a kiadások helye.');
  const response = await fetch(`https://github.com/${repository}/releases/latest/download/playful-keypad-release.json`, { cache: 'no-store', signal: AbortSignal.timeout(15000) });
  if (!response.ok) throw new Error('A kiadás nem érhető el.');
  return parseRelease(await response.json(), repository, currentCode);
}
