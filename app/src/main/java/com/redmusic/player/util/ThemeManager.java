package com.redmusic.player.util;

import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;

/** 主题管理：换皮肤（主色调）、白天/夜间模式、字体颜色 */
public class ThemeManager {

    /** 可选皮肤主色（0-4 预设；5 为自定义色） */
    public static final int[] SKIN_COLORS = {
            0xFFC20C0C, 0xFF1976D2, 0xFF2E7D32, 0xFF7B1FA2, 0xFFF57C00
    };
    public static final String[] SKIN_NAMES = {"红色", "蓝色", "绿色", "紫色", "橙色", "自定义"};

    /** 当前主色（自定义时用存储的自定义色） */
    public static int primaryColor(Context c) {
        int idx = Prefs.getInt("skin_idx", 0);
        if (idx >= 0 && idx < SKIN_COLORS.length) return SKIN_COLORS[idx];
        return Prefs.getInt("skin_custom", SKIN_COLORS[0]);
    }

    /** 切换皮肤（仅改主色，即时生效） */
    public static void setSkin(Context c, int idx) {
        if (idx < 0 || idx >= SKIN_COLORS.length + 1) idx = 0;
        Prefs.setInt("skin_idx", idx);
    }

    /** 设置自定义主色 */
    public static void setCustomColor(Context c, int color) {
        Prefs.setInt("skin_custom", color);
        Prefs.setInt("skin_idx", SKIN_COLORS.length); // 5 = 自定义
    }

    /** 是否夜间模式 */
    public static boolean isDark(Context c) {
        return Prefs.getBool("dark_mode", true);
    }

    /** 切换白天/夜间模式 */
    public static void setDark(Context c, boolean dark) {
        Prefs.setBool("dark_mode", dark);
        AppCompatDelegate.setDefaultNightMode(dark
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO);
    }

    /** 主文字颜色（设置里可选：跟随主题 / 白色 / 黑色） */
    public static int primaryTextColor(Context c) {
        int f = Prefs.getInt("font_color", 0);
        if (f == 1) return 0xFFFFFFFF;
        if (f == 2) return 0xFF212121;
        return isDark(c) ? 0xFFFFFFFF : 0xFF212121;
    }

    /** 次文字颜色 */
    public static int secondaryTextColor(Context c) {
        return isDark(c) ? 0xFFB3B3B3 : 0xFF888888;
    }

    /** 页面背景色 */
    public static int backgroundColor(Context c) {
        return isDark(c) ? 0xFF121212 : 0xFFF5F5F5;
    }

    /** 卡片背景色 */
    public static int cardColor(Context c) {
        return isDark(c) ? 0xFF1E1E1E : 0xFFFFFFFF;
    }
}
