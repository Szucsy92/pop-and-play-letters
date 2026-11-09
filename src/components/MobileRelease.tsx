import { useState } from 'react';
import { Capacitor } from '@capacitor/core';
import { Button } from '@/components/ui/button';
import { checkRelease, type Release } from '@/lib/mobile-release';
export async function exitNative() {
  if (Capacitor.getPlatform() === 'android') { const { App } = await import('@capacitor/app'); await App.exitApp(); }
}
export function MobileRelease() {
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);
  const [release, setRelease] = useState<Release | null>(null);
  if (!Capacitor.isNativePlatform()) return null;
  const version = import.meta.env.VITE_APP_VERSION || 'Helyi változat';
  const repository = import.meta.env.VITE_RELEASE_REPOSITORY || '';
  const currentCode = Number(import.meta.env.VITE_APP_VERSION_CODE || 0);
  const android = Capacitor.getPlatform() === 'android';
  return <div className="flex max-w-sm flex-col items-center gap-2 text-center">
    <p className="text-sm text-muted-foreground">Verzió: {version}</p>
    {android && <Button variant="secondary" disabled={busy} onClick={async () => {
      setBusy(true); setRelease(null); setMessage('');
      try { const next = await checkRelease(repository, currentCode); setRelease(next); setMessage(next ? `Új változat: ${next.version}` : 'A legújabb változat van a telefonon.'); }
      catch { setMessage('Nem érhető el frissítés. Ellenőrizd az internetet és próbáld újra később.'); }
      finally { setBusy(false); }
    }}>{busy ? 'Ellenőrzés…' : 'Frissítés keresése'}</Button>}
    <p role="status" className="text-sm text-muted-foreground">{message}</p>
    {release && <Button onClick={async () => {
      try { const { Browser } = await import('@capacitor/browser'); await Browser.open({ url: release.apkUrl }); }
      catch { setMessage('Nem sikerült megnyitni a letöltést.'); }
    }}>Új APK letöltése</Button>}
    {!android && <p className="text-sm text-muted-foreground">iPhone-frissítés: az új IPA ismételt telepítésével.</p>}
  </div>;
}
