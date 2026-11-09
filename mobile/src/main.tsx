import { createRoot } from 'react-dom/client';
import { App } from '@capacitor/app';
import { Capacitor } from '@capacitor/core';
import { StatusBar } from '@capacitor/status-bar';
import '@fontsource/fredoka/700.css';
import '@/styles.css';
import { KeypadGame } from '@/components/KeypadGame';
const root = document.getElementById('root');
if (root) createRoot(root).render(<KeypadGame />);
if (Capacitor.isNativePlatform()) {
  void StatusBar.hide().catch(() => {});
  if (Capacitor.getPlatform() === 'android') void App.addListener('backButton', () => {});
}
