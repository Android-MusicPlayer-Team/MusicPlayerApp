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
        // 独立设置页返回（退出）；颜色由 ic_arrow_back 的 text_primary 资源随黑白自动切换
        view.findViewById(R.id.settings_back).setOnClickListener(v -> {
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
            if (i < ThemeManager.SKIN_COLORS.length) {
                GradientDrawable gd = new GradientDrawable();
                gd.setShape(GradientDrawable.OVAL);
                gd.setColor(ThemeManager.SKIN_COLORS[i]);
                if (i == cur) gd.setStroke(dp(3), 0xFFFFFFFF);
                skinViews[i].setBackground(gd);
            } else {
                // 自定义入口：彩色色环（不固定单色）
                ColorRingDrawable ring = new ColorRingDrawable();
                ring.setSelected(i == cur);
                skinViews[i].setBackground(ring);
            }
        }
    }

    /** 自定义主色：弹出 HSV 色轮取色器 */
    private void showCustomPicker() {
        new ColorPickerDialog(requireContext(), ThemeManager.primaryColor(requireContext()), color -> {
            ThemeManager.setCustomColor(requireContext(), color);
            refreshSkinUi();
            applyTheme();
        }).show();
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
