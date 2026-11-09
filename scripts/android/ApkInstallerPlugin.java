package hu.playfulkeypad.app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.core.content.FileProvider;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** Letölti az új APK-t az appon belül, majd megnyitja a rendszer telepítőjét (ugyanazzal a kulccsal frissít). */
@CapacitorPlugin(name = "ApkInstaller")
public class ApkInstallerPlugin extends Plugin {

  @PluginMethod
  public void install(final PluginCall call) {
    final String url = call.getString("url");
    if (url == null || !url.startsWith("https://github.com/") || !url.endsWith(".apk")) {
      call.reject("bad_url");
      return;
    }
    final Context ctx = getContext();
    if (Build.VERSION.SDK_INT >= 26 && !ctx.getPackageManager().canRequestPackageInstalls()) {
      Intent s = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + ctx.getPackageName()));
      s.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      ctx.startActivity(s);
      call.reject("permission_needed");
      return;
    }
    new Thread(() -> {
      HttpURLConnection conn = null;
      try {
        File dir = new File(ctx.getCacheDir(), "updates");
        if (!dir.exists() && !dir.mkdirs()) throw new Exception("no_dir");
        File tmp = new File(dir, "update.part");
        File apk = new File(dir, "update.apk");
        conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);
        if (conn.getResponseCode() != 200) throw new Exception("http_" + conn.getResponseCode());
        long total = conn.getContentLengthLong();
        long done = 0;
        int lastPct = -1;
        try (InputStream in = conn.getInputStream(); FileOutputStream out = new FileOutputStream(tmp)) {
          byte[] buf = new byte[32768];
          int n;
          while ((n = in.read(buf)) > 0) {
            out.write(buf, 0, n);
            done += n;
            if (total > 0) {
              int pct = (int) (done * 100 / total);
              if (pct / 5 != lastPct / 5) {
                lastPct = pct;
                JSObject p = new JSObject();
                p.put("percent", pct);
                notifyListeners("progress", p);
              }
            }
          }
        }
        if (tmp.length() < 1024 * 1024) throw new Exception("too_small");
        if (apk.exists() && !apk.delete()) throw new Exception("cannot_replace");
        if (!tmp.renameTo(apk)) throw new Exception("rename_failed");
        Uri uri = FileProvider.getUriForFile(ctx, ctx.getPackageName() + ".fileprovider", apk);
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setDataAndType(uri, "application/vnd.android.package-archive");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(i);
        call.resolve();
      } catch (Exception e) {
        call.reject(e.getMessage() == null ? "download_failed" : e.getMessage());
      } finally {
        if (conn != null) conn.disconnect();
      }
    }).start();
  }
}
