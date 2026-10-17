package com.redmusic.player.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.redmusic.player.R;

/** 搜索页壳：从发现页右上角进入，承载 SearchFragment */
public class SearchActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.search_container, new SearchFragment())
                .commit();
    }
}
