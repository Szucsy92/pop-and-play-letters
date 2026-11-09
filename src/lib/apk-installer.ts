import { registerPlugin } from '@capacitor/core';
export type ApkInstallerPlugin = {
  install(options: { url: string }): Promise<void>;
  addListener(event: 'progress', fn: (e: { percent: number }) => void): Promise<{ remove: () => Promise<void> }>;
};
export const ApkInstaller = registerPlugin<ApkInstallerPlugin>('ApkInstaller');
