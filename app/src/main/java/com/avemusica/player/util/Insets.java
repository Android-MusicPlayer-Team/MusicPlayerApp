package com.avemusica.player.util;

import android.os.Build;
import android.view.View;
import android.view.WindowInsets;

/** 让页面内容避开状态栏与导航栏，避免全面屏/无边框机型上内容顶上、整体上移 */
public final class Insets {

    private Insets() {
    }

    public static void apply(View root) {
        if (root == null) return;
        final int left = root.getPaddingLeft();
        final int top = root.getPaddingTop();
        final int right = root.getPaddingRight();
        final int bottom = root.getPaddingBottom();
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int t;
            int b;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                t = bars.top;
                b = bars.bottom;
            } else {
                t = insets.getSystemWindowInsetTop();
                b = insets.getSystemWindowInsetBottom();
            }
            v.setPadding(left, top + t, right, bottom + b);
            return insets;
        });
        root.requestApplyInsets();
    }
}
