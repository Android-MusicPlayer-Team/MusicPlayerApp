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
