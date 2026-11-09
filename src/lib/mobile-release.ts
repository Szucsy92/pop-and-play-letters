export type Release = { versionCode: number; version: string; apkUrl: string };
export function parseRelease(data: unknown, repository: string, currentCode: number): Release | null {
  if (!data || typeof data !== 'object') throw new Error('Érvénytelen kiadás.');
  const row = data as Record<string, unknown>;
  if (typeof row.versionCode !== 'number' || !Number.isSafeInteger(row.versionCode) || row.versionCode < 1 || typeof row.version !== 'string' || !row.version || typeof row.apkUrl !== 'string') throw new Error('Hiányos verzióadatok.');
  const url = new URL(row.apkUrl);
  if (url.protocol !== 'https:' || url.hostname !== 'github.com' || !url.pathname.startsWith(`/${repository}/releases/download/`) || !url.pathname.endsWith('.apk') || url.username || url.password) throw new Error('Érvénytelen telepítőcím.');
  return row.versionCode > currentCode ? { versionCode: row.versionCode, version: row.version, apkUrl: row.apkUrl } : null;
}
export async function checkRelease(repository: string, currentCode: number): Promise<Release | null> {
  if (!/^[\w.-]+\/[\w.-]+$/.test(repository)) throw new Error('Nincs beállítva a kiadások helye.');
  const response = await fetch(`https://github.com/${repository}/releases/latest/download/playful-keypad-release.json`, { cache: 'no-store', signal: AbortSignal.timeout(15000) });
  if (!response.ok) throw new Error('A kiadás nem érhető el.');
  return parseRelease(await response.json(), repository, currentCode);
}
