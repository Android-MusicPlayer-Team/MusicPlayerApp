package com.redmusic.player;

import android.app.Application;

import com.redmusic.player.player.PlayerHolder;
import com.redmusic.player.util.Prefs;

public class App extends Application {
    private static App instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Prefs.init(this);
        PlayerHolder.init(this);
    }

    public static App get() {
        return instance;
    }
}
