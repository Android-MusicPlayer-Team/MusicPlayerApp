package com.redmusic.player.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.redmusic.player.R;
import com.redmusic.player.player.PlayerHolder;
import com.redmusic.player.util.ThemeManager;

public class MainActivity extends AppCompatActivity {

    private static final int TAB_HOME = 0;
    private static final int TAB_LIBRARY = 1;

    private int currentTab = -1;
    private MiniPlayerBar miniBar;
    private int lastThemeRev = -1;

    private ImageView homeIcon, libIcon;
    private TextView homeText, libText;
    private final Runnable playerListener = this::updateMiniBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FrameLayout container = findViewById(R.id.fragment_container);
        miniBar = findViewById(R.id.mini_bar);

        homeIcon = findViewById(R.id.nav_home_icon);
        libIcon = findViewById(R.id.nav_library_icon);
        homeText = findViewById(R.id.nav_home_text);
        libText = findViewById(R.id.nav_library_text);

        findViewById(R.id.nav_home).setOnClickListener(v -> selectTab(TAB_HOME));
        findViewById(R.id.nav_library).setOnClickListener(v -> selectTab(TAB_LIBRARY));

        miniBar.setOnClickListener(v -> {
            if (PlayerHolder.get().hasTrack()) {
                startActivity(new Intent(this, PlayerActivity.class));
            }
        });
        updateMiniBar();
        selectTab(TAB_HOME);
        requestNotifPermission();
        lastThemeRev = com.redmusic.player.util.Prefs.getInt("theme_rev", 0);
    }

    /** Android 13+ 请求通知权限（后台播放的通知栏控制需要） */
    private void requestNotifPermission() {
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
        }
    }

    private void selectTab(int tab) {
        if (currentTab == tab && getSupportFragmentManager().findFragmentById(R.id.fragment_container) != null) {
            return;
        }
        currentTab = tab;
        Fragment f;
        if (tab == TAB_LIBRARY) {
            f = new LibraryFragment();
        } else {
            f = new HomeFragment();
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, f)
                .commit();

        int primary = ThemeManager.primaryColor(this);
        int gray = 0xFF888888;
        homeIcon.setColorFilter(tab == TAB_HOME ? primary : gray);
        libIcon.setColorFilter(tab == TAB_LIBRARY ? primary : gray);
        homeText.setTextColor(tab == TAB_HOME ? primary : gray);
        libText.setTextColor(tab == TAB_LIBRARY ? primary : gray);
    }

    private void updateMiniBar() {
        if (miniBar != null) miniBar.refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        PlayerHolder.get().addListener(playerListener);
        updateMiniBar();
        // 主题在设置页变更后，回到主界面时同步重建（换肤/日夜生效）
        int rev = com.redmusic.player.util.Prefs.getInt("theme_rev", 0);
        if (lastThemeRev >= 0 && rev != lastThemeRev) {
            lastThemeRev = rev;
            recreate();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        PlayerHolder.get().removeListener(playerListener);
    }
}
