package com.avemusica.player.util;

import android.app.Activity;
import android.graphics.Color;
import android.view.Window;
import android.view.WindowManager;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * 系统栏：状态栏透明、由页面自己负责色块（Android 15 起系统会接管状态栏底色，
 * 只有让内容延伸到状态栏，系统才不会叠一层深色）；导航栏仍由本类着色。
 */
public final class SystemBars {

    private SystemBars() {
    }

    public static void setup(Activity a) {
        Window w = a.getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        // 内容延伸到系统栏，内边距由 Insets 按原始 padding 叠加
        WindowCompat.setDecorFitsSystemWindows(w, false);

        int bg = ThemeManager.backgroundColor(a);
        w.setStatusBarColor(Color.TRANSPARENT);
        w.setNavigationBarColor(bg);

        boolean lightBackground = Color.luminance(bg) > 0.5f;
        WindowInsetsControllerCompat c = WindowCompat.getInsetsController(w, w.getDecorView());
        c.setAppearanceLightStatusBars(lightBackground);
        c.setAppearanceLightNavigationBars(lightBackground);
    }
}
