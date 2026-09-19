package com.redmusic.player.ui;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.audiofx.Visualizer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.palette.graphics.Palette;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.redmusic.player.R;
import com.redmusic.player.api.MusicApi;
import com.redmusic.player.model.LyricLine;
import com.redmusic.player.model.Track;
import com.redmusic.player.player.PlayerHolder;
import com.redmusic.player.util.Prefs;

import java.util.List;

public class PlayerActivity extends AppCompatActivity {

    private static final int REQ_AUDIO = 100;

    private final Handler main = new Handler(Looper.getMainLooper());
    private final Runnable progressTick = new Runnable() {
        @Override
        public void run() {
            updateProgress();
            main.postDelayed(this, 500);
        }
    };

    private ImageView cover, bgBlur;
    private View bgGradient;
    private TextView title, artist, curTime, totalTime, lyricLine, speedBtn, timerBtn, lyricToggle;
    private SeekBar seekBar;
    private ImageButton shuffleBtn, prevBtn, playBtn, nextBtn, repeatBtn, favBtn;
    private LyricView lyricView;
    private SpectrumView spectrum;
    private ObjectAnimator rotateAnim;
    private Visualizer visualizer;

    private final Runnable playerListener = this::refreshAll;
    private final Runnable sleepTick = new Runnable() {
        @Override
        public void run() {
            if (sleepAt > 0 && System.currentTimeMillis() >= sleepAt) {
                sleepAt = 0;
                PlayerHolder.get().getPlayer().pause();
                timerBtn.setText("睡眠");
                Toast.makeText(PlayerActivity.this, "定时已到，播放已暂停", Toast.LENGTH_SHORT).show();
            }
            main.postDelayed(this, 1000);
        }
    };
    private long sleepAt = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        cover = findViewById(R.id.player_cover);
        bgBlur = findViewById(R.id.player_bg_blur);
        bgGradient = findViewById(R.id.player_bg_gradient);
        title = findViewById(R.id.player_title);
        artist = findViewById(R.id.player_artist);
        curTime = findViewById(R.id.player_cur_time);
        totalTime = findViewById(R.id.player_total_time);
        lyricLine = findViewById(R.id.player_lyric_line);
        seekBar = findViewById(R.id.player_seek);
        shuffleBtn = findViewById(R.id.player_shuffle);
        prevBtn = findViewById(R.id.player_prev);
        playBtn = findViewById(R.id.player_play);
        nextBtn = findViewById(R.id.player_next);
        repeatBtn = findViewById(R.id.player_repeat);
        favBtn = findViewById(R.id.player_fav);
        speedBtn = findViewById(R.id.player_speed);
        timerBtn = findViewById(R.id.player_timer);
        lyricToggle = findViewById(R.id.player_lyric_toggle);
        lyricView = findViewById(R.id.player_lyric_view);
        spectrum = findViewById(R.id.player_spectrum);

        rotateAnim = ObjectAnimator.ofFloat(cover, "rotation", 0f, 360f);
        rotateAnim.setDuration(20000);
        rotateAnim.setRepeatCount(ValueAnimator.INFINITE);
        rotateAnim.setInterpolator(new LinearInterpolator());

        findViewById(R.id.player_back).setOnClickListener(v -> finish());
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    PlayerHolder.get().seekTo(progress);
                    curTime.setText(fmt(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        shuffleBtn.setOnClickListener(v -> {
            PlayerHolder.get().toggleShuffle();
            refreshControls();
            Toast.makeText(this, PlayerHolder.get().isShuffle() ? "已开启随机播放" : "已关闭随机播放", Toast.LENGTH_SHORT).show();
        });
        prevBtn.setOnClickListener(v -> PlayerHolder.get().prev());
        playBtn.setOnClickListener(v -> PlayerHolder.get().playPause());
        nextBtn.setOnClickListener(v -> PlayerHolder.get().next(false));
        repeatBtn.setOnClickListener(v -> {
            PlayerHolder.get().cycleRepeat();
            refreshControls();
        });
        favBtn.setOnClickListener(v -> {
            Track t = PlayerHolder.get().currentTrack();
            if (t == null) return;
            Prefs.toggleFav(t);
            updateFav();
            Toast.makeText(this, Prefs.isFav(t) ? "已收藏" : "已取消收藏", Toast.LENGTH_SHORT).show();
        });
        speedBtn.setOnClickListener(v -> {
            PlayerHolder ph = PlayerHolder.get();
            float s = ph.getSpeed();
            float next = s >= 1.99f ? 0.5f : s + 0.25f;
            if (s >= 1.99f) next = 0.5f;
            else if (s >= 1.74f) next = 2.0f;
            else next = s + 0.25f;
            ph.setSpeed(next);
            speedBtn.setText(String.format("%.2fx", next));
        });
        timerBtn.setOnClickListener(v -> showTimerDialog());
        lyricToggle.setOnClickListener(v -> {
            boolean fs = !lyricView.isFullScreen();
            lyricView.setFullScreen(fs);
            if (fs) {
                cover.setVisibility(View.INVISIBLE);
                spectrum.setVisibility(View.INVISIBLE);
                lyricLine.setVisibility(View.GONE);
                lyricView.setVisibility(View.VISIBLE);
                lyricToggle.setText("封面");
            } else {
                cover.setVisibility(View.VISIBLE);
                spectrum.setVisibility(View.VISIBLE);
                lyricLine.setVisibility(View.VISIBLE);
                lyricView.setVisibility(View.GONE);
                lyricToggle.setText("歌词");
            }
        });

        requestAudioPermission();
        main.post(progressTick);
        main.post(sleepTick);
    }

    @Override
    protected void onResume() {
        super.onResume();
        PlayerHolder.get().addListener(playerListener);
        refreshAll();
        main.post(progressTick);
        updateSpectrumMode();
    }

    @Override
    protected void onPause() {
        super.onPause();
        PlayerHolder.get().removeListener(playerListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        main.removeCallbacks(progressTick);
        main.removeCallbacks(sleepTick);
        releaseVisualizer();
    }

    private void requestAudioPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
            } else {
                setupVisualizer();
            }
        } else {
            setupVisualizer();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupVisualizer();
            } else {
                spectrum.startAnim();
            }
        }
    }

    private void setupVisualizer() {
        try {
            releaseVisualizer();
            visualizer = new Visualizer(PlayerHolder.get().getAudioSessionId());
            int[] range = Visualizer.getCaptureSizeRange();
            visualizer.setCaptureSize(Math.min(range[1], 256));
            visualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
                @Override
                public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
                    spectrum.setWaveform(waveform);
                }

                @Override
                public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
                }
            }, Visualizer.getMaxCaptureRate() / 2, true, false);
            visualizer.setEnabled(true);
        } catch (Exception e) {
            spectrum.startAnim();
        }
    }

    private void releaseVisualizer() {
        if (visualizer != null) {
            try {
                visualizer.setEnabled(false);
                visualizer.release();
            } catch (Exception ignored) {
            }
            visualizer = null;
        }
    }

    private void updateSpectrumMode() {
        PlayerHolder ph = PlayerHolder.get();
        if (ph.isPlaying()) {
            spectrum.startAnim();
        } else {
            spectrum.stopAnim();
        }
    }

    private void refreshAll() {
        PlayerHolder ph = PlayerHolder.get();
        Track t = ph.currentTrack();
        if (t == null) {
            cover.setImageResource(R.drawable.ic_music_note);
            title.setText("未在播放");
            artist.setText("");
            lyricLine.setText("");
            return;
        }
        title.setText(t.displayTitle());
        artist.setText(t.displayArtist() + (t.album != null && !t.album.isEmpty() ? " · " + t.album : ""));
        if (t.artworkUrl != null && !t.artworkUrl.isEmpty()) {
            RequestOptions opts = new RequestOptions()
                    .placeholder(R.drawable.ic_music_note)
                    .transform(new RoundedCorners(dp(28)));
            Glide.with(this).load(t.artworkUrl).apply(opts).into(cover);
            Glide.with(this).load(t.artworkUrl)
                    .into(new CustomTarget<android.graphics.drawable.Drawable>() {
                        @Override
                        public void onResourceReady(@NonNull Drawable resource, Transition<? super Drawable> transition) {
                            bgBlur.setImageDrawable(resource);
                            if (resource instanceof BitmapDrawable) {
                                Bitmap bmp = ((BitmapDrawable) resource).getBitmap();
                                if (bmp != null) {
                                    try {
                                        Palette.from(bmp).generate(p -> {
                                            if (p != null) {
                                                int c = p.getVibrantColor(0xFFC20C0C);
                                                applyBg(c);
                                            }
                                        });
                                    } catch (Exception ignored) {
                                    }
                                }
                            }
                        }

                        @Override
                        public void onLoadCleared(Drawable placeholder) {
                        }
                    });
        } else {
            cover.setImageResource(R.drawable.ic_music_note);
            applyBg(0xFFC20C0C);
        }
        updateFav();
        refreshControls();
        updateProgress();
        updateSpectrumMode();

        // 封面旋转
        if (rotateAnim != null && !rotateAnim.isRunning() && ph.isPlaying()) {
            rotateAnim.start();
        }
        if (ph.isPlaying() && rotateAnim != null && rotateAnim.isPaused()) {
            rotateAnim.resume();
        }
        if (!ph.isPlaying() && rotateAnim != null && rotateAnim.isRunning()) {
            rotateAnim.pause();
        }
    }

    private void applyBg(int color) {
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.argb(230, Color.red(color), Color.green(color), Color.blue(color)),
                        0xFF111111});
        gd.setCornerRadius(0);
        bgGradient.setBackground(gd);
    }

    private void updateFav() {
        Track t = PlayerHolder.get().currentTrack();
        if (t == null) return;
        favBtn.setImageResource(Prefs.isFav(t) ? R.drawable.ic_heart_filled : R.drawable.ic_heart);
    }

    private void refreshControls() {
        PlayerHolder ph = PlayerHolder.get();
        playBtn.setImageResource(ph.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
        shuffleBtn.setImageResource(ph.isShuffle() ? R.drawable.ic_shuffle_on : R.drawable.ic_shuffle);
        int rm = ph.getRepeatMode();
        repeatBtn.setImageResource(rm == 1 ? R.drawable.ic_repeat_one : R.drawable.ic_repeat);
        repeatBtn.setColorFilter(rm == 0 ? 0xFF888888 : 0xFFC20C0C);
        shuffleBtn.setColorFilter(ph.isShuffle() ? 0xFFC20C0C : 0xFF888888);
    }

    private void updateProgress() {
        PlayerHolder ph = PlayerHolder.get();
        long pos = ph.getPosition();
        long dur = ph.getDuration();
        if (dur <= 0) {
            seekBar.setMax(1000);
            seekBar.setProgress(0);
            curTime.setText(fmt(pos));
            totalTime.setText("--:--");
        } else {
            seekBar.setMax((int) dur);
            seekBar.setProgress((int) pos);
            curTime.setText(fmt(pos));
            totalTime.setText(fmt(dur));
        }
        Track t = ph.currentTrack();
        lyricView.updatePosition(pos);
        // 当前歌词行显示
        if (t != null && currentLines != null && !currentLines.isEmpty()) {
            String line = "";
            for (LyricLine l : currentLines) {
                if (pos >= l.timeMs) line = l.text;
            }
            lyricLine.setText(line);
        } else {
            lyricLine.setText("");
        }
    }

    private List<LyricLine> currentLines;

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // 歌曲切换后拉取歌词
        Track t = PlayerHolder.get().currentTrack();
        if (t == null) return;
        MusicApi.loadLyrics(t, new MusicApi.LyricCallback() {
            @Override
            public void onSuccess(List<LyricLine> lines) {
                runOnUiThread(() -> {
                    currentLines = lines;
                    lyricView.setLines(lines);
                    lyricView.updatePosition(PlayerHolder.get().getPosition());
                });
            }

            @Override
            public void onError(String msg) {
                runOnUiThread(() -> {
                    currentLines = null;
                    lyricView.setLines(null);
                });
            }
        });
    }

    private void showTimerDialog() {
        String[] options = {"15 分钟", "30 分钟", "60 分钟", "取消定时"};
        android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(this);
        b.setTitle("睡眠定时");
        b.setItems(options, (d, which) -> {
            if (which == 0) {
                sleepAt = System.currentTimeMillis() + 15 * 60000L;
                timerBtn.setText("15分钟后停止");
            } else if (which == 1) {
                sleepAt = System.currentTimeMillis() + 30 * 60000L;
                timerBtn.setText("30分钟后停止");
            } else if (which == 2) {
                sleepAt = System.currentTimeMillis() + 60 * 60000L;
                timerBtn.setText("60分钟后停止");
            } else {
                sleepAt = 0;
                timerBtn.setText("睡眠");
            }
            if (sleepAt > 0) {
                Toast.makeText(this, "已设置睡眠定时", Toast.LENGTH_SHORT).show();
            }
        });
        b.show();
    }

    private String fmt(long ms) {
        long s = ms / 1000;
        return String.format("%d:%02d", s / 60, s % 60);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
