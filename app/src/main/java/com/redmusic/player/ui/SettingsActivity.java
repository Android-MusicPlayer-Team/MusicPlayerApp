package com.redmusic.player.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.redmusic.player.R;

/** 设置页壳：从"我的"右上角进入，承载 SettingsFragment，可返回退出 */
public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings_container, new SettingsFragment())
                .commit();
    }
}
