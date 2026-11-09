import { copyFileSync, existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
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

  // Offline, appon belüli frissítés: telepítő plugin + MainActivity + engedélyek.
  const javaDir = 'android/app/src/main/java/hu/playfulkeypad/app';
  mkdirSync(javaDir, { recursive: true });
  copyFileSync('scripts/android/MainActivity.java', `${javaDir}/MainActivity.java`);
  copyFileSync('scripts/android/ApkInstallerPlugin.java', `${javaDir}/ApkInstallerPlugin.java`);
  mkdirSync('android/app/src/main/res/xml', { recursive: true });
  writeFileSync('android/app/src/main/res/xml/file_paths.xml', '<?xml version="1.0" encoding="utf-8"?>\n<paths xmlns:android="http://schemas.android.com/apk/res/android">\n  <cache-path name="updates" path="updates/" />\n  <cache-path name="cache_all" path="." />\n  <external-path name="external_all" path="." />\n</paths>\n');

  const mpath = 'android/app/src/main/AndroidManifest.xml';
  let m = readFileSync(mpath, 'utf8');
  if (!m.includes('REQUEST_INSTALL_PACKAGES')) m = m.replace('<application', '<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />\n    <application');
  if (!m.includes('android.permission.INTERNET')) m = m.replace('<application', '<uses-permission android:name="android.permission.INTERNET" />\n    <application');
  if (!m.includes('androidx.core.content.FileProvider')) m = m.replace('</application>', '  <provider android:name="androidx.core.content.FileProvider" android:authorities="${applicationId}.fileprovider" android:exported="false" android:grantUriPermissions="true"><meta-data android:name="android.support.FILE_PROVIDER_PATHS" android:resource="@xml/file_paths" /></provider>\n    </application>');
  // A rendszerbillentyűzet felnyílásakor az ablak kisebb lesz, így a betű a látható területen marad.
  if (/android:windowSoftInputMode="[^"]*"/.test(m)) m = m.replace(/android:windowSoftInputMode="[^"]*"/, 'android:windowSoftInputMode="adjustResize"');
  else m = m.replace('<activity', '<activity android:windowSoftInputMode="adjustResize"');
  if (!m.includes('REQUEST_INSTALL_PACKAGES') || !m.includes('adjustResize') || !m.includes('FileProvider')) throw new Error('Manifest patch failed');
  writeFileSync(mpath, m);
} else if (platform === 'ios') {
  const path = 'ios/App/App.xcodeproj/project.pbxproj';
  let text = readFileSync(path, 'utf8');
  if (!text.includes('MARKETING_VERSION =') || !text.includes('CURRENT_PROJECT_VERSION =')) throw new Error('iOS version fields not found');
  // Apple's marketing version is numeric; date/time also lives in filename/release title.
  text = text.replace(/MARKETING_VERSION = [^;]+;/g, `MARKETING_VERSION = ${version.slice(0, 10).split('.').map(Number).join('.')};`).replace(/CURRENT_PROJECT_VERSION = [^;]+;/g, `CURRENT_PROJECT_VERSION = ${code};`);
  writeFileSync(path, text);
} else throw new Error('Unknown platform');
