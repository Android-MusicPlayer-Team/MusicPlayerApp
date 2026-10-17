package com.redmusic.player;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.redmusic.player.player.PlayerHolder;
import com.redmusic.player.util.Prefs;
import com.redmusic.player.util.ThemeManager;

public class App extends Application {
    private static App instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Prefs.init(this);

        // 全局崩溃捕获：把崩溃堆栈写到 应用外部文件目录/crash.log，便于定位黑屏/闪退
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            try {
                java.io.StringWriter sw = new java.io.StringWriter();
                e.printStackTrace(new java.io.PrintWriter(sw));
                java.io.File f = new java.io.File(getExternalFilesDir(null), "crash.log");
                try (java.io.FileWriter fw = new java.io.FileWriter(f, true)) {
                    fw.write(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                            .format(new java.util.Date()) + "\n" + sw + "\n====\n");
                }
            } catch (Exception ignored) {
            }
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(10);
        });

        // 应用白天/夜间模式偏好
        AppCompatDelegate.setDefaultNightMode(ThemeManager.isDark(this)
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO);
        PlayerHolder.init(this);
    }

    public static App get() {
        return instance;
    }
}
