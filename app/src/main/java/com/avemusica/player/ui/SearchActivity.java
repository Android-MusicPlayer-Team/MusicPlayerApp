package com.avemusica.player.ui;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.avemusica.player.R;
import com.avemusica.player.util.Insets;
import com.avemusica.player.util.SystemBars;
import com.avemusica.player.util.ThemeManager;
import com.avemusica.player.util.ThemeUi;

/** 搜索页壳：从发现页右上角进入，承载 SearchFragment */
public class SearchActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SystemBars.setup(this);
        setContentView(R.layout.activity_search);
        View content = findViewById(android.R.id.content);
        content.setBackgroundColor(ThemeManager.backgroundColor(this));
        Insets.apply(content);
        content.post(() -> ThemeUi.apply(content));
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.search_container, new SearchFragment())
                .commit();
    }
}
