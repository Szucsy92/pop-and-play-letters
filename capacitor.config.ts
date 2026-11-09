import type { CapacitorConfig } from '@capacitor/cli';
const config: CapacitorConfig = {
  appId: 'hu.playfulkeypad.app', appName: 'Gyerek billentyűzet', webDir: 'dist-mobile',
  backgroundColor: '#1e1b2e', plugins: { StatusBar: { overlaysWebView: true } },
};
export default config;
