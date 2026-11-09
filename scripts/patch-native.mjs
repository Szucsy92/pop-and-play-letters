import { readFileSync, writeFileSync } from 'node:fs';
const code = Number(process.env.APP_VERSION_CODE);
const version = process.env.APP_VERSION_NAME;
if (!Number.isSafeInteger(code) || code < 1 || !/^\d{4}\.\d{2}\.\d{2}-\d{4}$/.test(version || '')) throw new Error('Missing build version');
const platform = process.argv[2];
if (platform === 'android') {
  const path = 'android/app/build.gradle';
  let text = readFileSync(path, 'utf8');
  if (!/versionCode\s+\d+/.test(text) || !/versionName\s+"[^"]+"/.test(text)) throw new Error('Android version fields not found');
  text = text.replace(/versionCode\s+\d+/, `versionCode ${code}`).replace(/versionName\s+"[^"]+"/, `versionName "${version}"`);
  writeFileSync(path, text);
} else if (platform === 'ios') {
  const path = 'ios/App/App.xcodeproj/project.pbxproj';
  let text = readFileSync(path, 'utf8');
  if (!text.includes('MARKETING_VERSION =') || !text.includes('CURRENT_PROJECT_VERSION =')) throw new Error('iOS version fields not found');
  // Apple's marketing version is numeric; date/time also lives in filename/release title.
  text = text.replace(/MARKETING_VERSION = [^;]+;/g, `MARKETING_VERSION = ${version.slice(0, 10).split('.').map(Number).join('.')};`).replace(/CURRENT_PROJECT_VERSION = [^;]+;/g, `CURRENT_PROJECT_VERSION = ${code};`);
  writeFileSync(path, text);
} else throw new Error('Unknown platform');
