package com.redmusic.player.ui;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.redmusic.player.R;
import com.redmusic.player.util.Prefs;
import com.redmusic.player.util.ThemeManager;

/** 设置页：换皮肤（主色）、白天/夜间、字体颜色 */
public class SettingsFragment extends Fragment {

    private View[] skinViews;
    private TextView modeDay, modeNight, fontAuto, fontWhite, fontBlack;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // 独立设置页返回（退出）
        android.widget.ImageButton backBtn = view.findViewById(R.id.settings_back);
        backBtn.setColorFilter(ThemeManager.primaryTextColor(requireContext()));
        backBtn.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().finish();
        });

        skinViews = new View[]{
                view.findViewById(R.id.skin_0),
                view.findViewById(R.id.skin_1),
                view.findViewById(R.id.skin_2),
                view.findViewById(R.id.skin_3),
                view.findViewById(R.id.skin_4),
                view.findViewById(R.id.skin_5)
        };
        for (int i = 0; i < skinViews.length; i++) {
            final int idx = i;
            skinViews[i].setOnClickListener(v -> {
                if (idx < ThemeManager.SKIN_COLORS.length) {
                    ThemeManager.setSkin(requireContext(), idx);
                    refreshSkinUi();
                    applyTheme();
                } else {
                    showCustomPicker();
                }
            });
        }

        modeDay = view.findViewById(R.id.mode_day);
        modeNight = view.findViewById(R.id.mode_night);
        modeDay.setOnClickListener(v -> {
            ThemeManager.setDark(requireContext(), false);
            applyTheme();
        });
        modeNight.setOnClickListener(v -> {
            ThemeManager.setDark(requireContext(), true);
            applyTheme();
        });

        fontAuto = view.findViewById(R.id.font_auto);
        fontWhite = view.findViewById(R.id.font_white);
        fontBlack = view.findViewById(R.id.font_black);
        fontAuto.setOnClickListener(v -> { Prefs.setInt("font_color", 0); refreshFontUi(); });
        fontWhite.setOnClickListener(v -> { Prefs.setInt("font_color", 1); refreshFontUi(); });
        fontBlack.setOnClickListener(v -> { Prefs.setInt("font_color", 2); refreshFontUi(); });

        refreshSkinUi();
        refreshModeUi();
        refreshFontUi();
    }

    private void refreshSkinUi() {
        int cur = Prefs.getInt("skin_idx", 0);
        for (int i = 0; i < skinViews.length; i++) {
            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.OVAL);
            if (i < ThemeManager.SKIN_COLORS.length) {
                gd.setColor(ThemeManager.SKIN_COLORS[i]);
            } else {
                // 自定义圆点：显示当前自定义色（未设置时灰色）
                gd.setColor(ThemeManager.primaryColor(requireContext()));
            }
            if (i == cur) gd.setStroke(dp(3), 0xFFFFFFFF);
            skinViews[i].setBackground(gd);
        }
    }

    /** 自定义主色：输入 6 位 hex 颜色（如 C20C0C） */
    private void showCustomPicker() {
        androidx.appcompat.app.AlertDialog.Builder b = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        b.setTitle("自定义主题色");
        b.setMessage("输入 6 位十六进制颜色，例如：C20C0C（红）、1976D2（蓝）");
        final android.widget.EditText et = new android.widget.EditText(requireContext());
        et.setHint("C20C0C");
        b.setView(et);
        b.setPositiveButton("确定", (d, w) -> {
            String hex = et.getText().toString().trim().replace("#", "");
            try {
                if (hex.length() == 6) {
                    int color = android.graphics.Color.parseColor("#" + hex);
                    ThemeManager.setCustomColor(requireContext(), color);
                    refreshSkinUi();
                    applyTheme();
                } else {
                    android.widget.Toast.makeText(requireContext(), "颜色格式不正确", android.widget.Toast.LENGTH_SHORT).show();
                }
            } catch (Exception ex) {
                android.widget.Toast.makeText(requireContext(), "颜色格式不正确", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
        b.setNegativeButton("取消", null);
        b.show();
    }

    private void refreshModeUi() {
        boolean dark = ThemeManager.isDark(requireContext());
        highlight(modeDay, !dark);
        highlight(modeNight, dark);
    }

    private void refreshFontUi() {
        int f = Prefs.getInt("font_color", 0);
        highlight(fontAuto, f == 0);
        highlight(fontWhite, f == 1);
        highlight(fontBlack, f == 2);
    }

    private void highlight(TextView tv, boolean on) {
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(dp(22));
        if (on) {
            gd.setColor(ThemeManager.primaryColor(requireContext()));
            tv.setTextColor(0xFFFFFFFF);
        } else {
            gd.setColor(0x00000000);
            tv.setTextColor(0xFFB3B3B3);
        }
        tv.setBackground(gd);
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    /** 主题已变，整体重建 MainActivity 使皮肤/日夜生效 */
    private void applyTheme() {
        if (getActivity() != null) getActivity().recreate();
    }
}
