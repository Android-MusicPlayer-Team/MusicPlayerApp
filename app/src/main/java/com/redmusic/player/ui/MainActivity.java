package com.redmusic.player.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.redmusic.player.R;
import com.redmusic.player.player.PlayerHolder;

public class MainActivity extends AppCompatActivity {

    private static final int TAB_HOME = 0;
    private static final int TAB_SEARCH = 1;
    private static final int TAB_LIBRARY = 2;

    private int currentTab = -1;
    private MiniPlayerBar miniBar;

    private ImageView homeIcon, searchIcon, libIcon;
    private TextView homeText, searchText, libText;
    private final Runnable playerListener = this::updateMiniBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FrameLayout container = findViewById(R.id.fragment_container);
        miniBar = findViewById(R.id.mini_bar);

        homeIcon = findViewById(R.id.nav_home_icon);
        searchIcon = findViewById(R.id.nav_search_icon);
        libIcon = findViewById(R.id.nav_library_icon);
        homeText = findViewById(R.id.nav_home_text);
        searchText = findViewById(R.id.nav_search_text);
        libText = findViewById(R.id.nav_library_text);

        findViewById(R.id.nav_home).setOnClickListener(v -> selectTab(TAB_HOME));
        findViewById(R.id.nav_search).setOnClickListener(v -> selectTab(TAB_SEARCH));
        findViewById(R.id.nav_library).setOnClickListener(v -> selectTab(TAB_LIBRARY));

        miniBar.setOnClickListener(v -> {
            if (PlayerHolder.get().hasTrack()) {
                startActivity(new Intent(this, PlayerActivity.class));
            }
        });
        updateMiniBar();
        selectTab(TAB_HOME);
    }

    private void selectTab(int tab) {
        if (currentTab == tab && getSupportFragmentManager().findFragmentById(R.id.fragment_container) != null) {
            return;
        }
        currentTab = tab;
        Fragment f;
        if (tab == TAB_SEARCH) {
            f = new SearchFragment();
        } else if (tab == TAB_LIBRARY) {
            f = new LibraryFragment();
        } else {
            f = new HomeFragment();
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, f)
                .commit();

        int red = 0xFFC20C0C;
        int gray = 0xFF888888;
        homeIcon.setColorFilter(tab == TAB_HOME ? red : gray);
        searchIcon.setColorFilter(tab == TAB_SEARCH ? red : gray);
        libIcon.setColorFilter(tab == TAB_LIBRARY ? red : gray);
        homeText.setTextColor(tab == TAB_HOME ? red : gray);
        searchText.setTextColor(tab == TAB_SEARCH ? red : gray);
        libText.setTextColor(tab == TAB_LIBRARY ? red : gray);
    }

    private void updateMiniBar() {
        if (miniBar != null) miniBar.refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        PlayerHolder.get().addListener(playerListener);
        updateMiniBar();
    }

    @Override
    protected void onPause() {
        super.onPause();
        PlayerHolder.get().removeListener(playerListener);
    }
}
