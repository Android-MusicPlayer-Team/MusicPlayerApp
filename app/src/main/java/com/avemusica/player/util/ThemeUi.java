package com.avemusica.player.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Switch;

import androidx.appcompat.widget.SwitchCompat;

/** 系统控件的强调色统一跟随皮肤主色：复选框、单选框、开关、多选勾选标记、进度条 */
public final class ThemeUi {

    private ThemeUi() {
    }

    /** 递归给整棵视图树套上当前主色 */
    public static void apply(View root) {
        if (root == null) return;
        Context c = root.getContext();
        if (c == null) return;
        tint(root, c);
        if (root instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) root;
            for (int i = 0; i < g.getChildCount(); i++) {
                apply(g.getChildAt(i));
            }
        }
    }

    /** 弹窗：统一按钮文字色，并在布局完成后给弹窗内控件上色 */
    public static void applyDialog(android.app.AlertDialog d) {
        if (d == null) return;
        final Context c = d.getContext();
        d.setOnShowListener(dlg -> {
            int pc = ThemeManager.primaryColor(c);
            try {
                Button pos = d.getButton(AlertDialog.BUTTON_POSITIVE);
                if (pos != null) pos.setTextColor(pc);
                Button neg = d.getButton(AlertDialog.BUTTON_NEGATIVE);
                if (neg != null) neg.setTextColor(pc);
                Button neu = d.getButton(AlertDialog.BUTTON_NEUTRAL);
                if (neu != null) neu.setTextColor(pc);
            } catch (Exception ignored) {
            }
            View decor = d.getWindow() == null ? null : d.getWindow().getDecorView();
            if (decor != null) {
                final View target = decor;
                target.post(() -> {
                    apply(target);
                    hookList(target, c);
                });
            }
        });
    }

    /** 多选列表滚动复用行时重新上色，避免新出现的勾选标记退回默认色 */
    private static void hookList(View root, Context c) {
        if (root instanceof android.widget.AbsListView) {
            ((android.widget.AbsListView) root).setOnScrollListener(
                    new android.widget.AbsListView.OnScrollListener() {
                        @Override
                        public void onScrollStateChanged(android.widget.AbsListView v, int state) {
                        }

                        @Override
                        public void onScroll(android.widget.AbsListView v, int first,
                                             int visible, int total) {
                            for (int i = 0; i < v.getChildCount(); i++) {
                                tint(v.getChildAt(i), c);
                            }
                        }
                    });
        }
        if (root instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) root;
            for (int i = 0; i < g.getChildCount(); i++) {
                hookList(g.getChildAt(i), c);
            }
        }
    }

    private static void tint(View v, Context c) {
        int pc = ThemeManager.primaryColor(c);
        ColorStateList cs = ColorStateList.valueOf(pc);
        try {
            if (v instanceof CheckBox) {
                ((CheckBox) v).setButtonTintList(cs);
            } else if (v instanceof RadioButton) {
                ((RadioButton) v).setButtonTintList(cs);
            } else if (v instanceof SwitchCompat) {
                SwitchCompat s = (SwitchCompat) v;
                s.setThumbTintList(cs);
                s.setTrackTintList(cs);
            } else if (v instanceof Switch) {
                Switch s = (Switch) v;
                s.setThumbTintList(cs);
                s.setTrackTintList(cs);
            } else if (v instanceof CheckedTextView) {
                ((CheckedTextView) v).setCheckMarkTintList(cs);
            } else if (v instanceof ProgressBar && !(v instanceof android.widget.SeekBar)) {
                ((ProgressBar) v).setProgressTintList(cs);
                ((ProgressBar) v).setIndeterminateTintList(cs);
            }
        } catch (Exception ignored) {
        }
    }
}
