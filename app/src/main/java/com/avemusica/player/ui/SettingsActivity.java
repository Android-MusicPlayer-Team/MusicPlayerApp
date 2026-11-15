package com.avemusica.player.ui;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.avemusica.player.R;
import com.avemusica.player.util.Insets;
import com.avemusica.player.util.SystemBars;
import com.avemusica.player.util.ThemeManager;
import com.avemusica.player.util.ThemeUi;

/** 设置页壳：从"我的"右上角进入，承载 SettingsFragment，可返回退出 */
public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SystemBars.setup(this);
        setContentView(R.layout.activity_settings);
        View content = findViewById(android.R.id.content);
        content.setBackgroundColor(ThemeManager.backgroundColor(this));
        Insets.apply(content);
        content.post(() -> ThemeUi.apply(content));
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings_container, new SettingsFragment())
                .commit();
    }
}
