package com.redmusic.player.player;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.app.NotificationCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.redmusic.player.R;
import com.redmusic.player.model.Track;
import com.redmusic.player.ui.PlayerActivity;

/** 后台播放前台服务：锁屏/后台继续播放，通知栏提供播放控制 */
public class PlaybackService extends Service {

    private static final String CHANNEL_ID = "playback";
    private static final int NOTIF_ID = 1;

    public static final String ACTION_PLAY_PAUSE = "com.redmusic.player.PLAY_PAUSE";
    public static final String ACTION_NEXT = "com.redmusic.player.NEXT";
    public static final String ACTION_PREV = "com.redmusic.player.PREV";

    private PowerManager.WakeLock wakeLock;
    private boolean foreground = false;
    /** 已加载封面的歌曲，避免重复加载 */
    private Track lastArtworkTrack = null;

    /** 开始播放时启动前台服务 */
    public static void start(Context ctx) {
        ContextCompat.startForegroundService(ctx, new Intent(ctx, PlaybackService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "redmusic:playback");
        wakeLock.setReferenceCounted(false);
        wakeLock.acquire();
        PlayerHolder.get().addListener(this::onPlaybackChanged);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        if (ACTION_PLAY_PAUSE.equals(action)) {
            PlayerHolder.get().playPause();
        } else if (ACTION_NEXT.equals(action)) {
            PlayerHolder.get().next(false);
        } else if (ACTION_PREV.equals(action)) {
            PlayerHolder.get().prev();
        }
        if (PlayerHolder.get().currentTrack() == null) {
            stopEverything();
            return START_NOT_STICKY;
        }
        showForeground();
        return START_STICKY;
    }

    /** 播放状态变化（切歌/播放暂停）时更新通知，没歌了则停止服务 */
    private void onPlaybackChanged() {
        if (PlayerHolder.get().currentTrack() == null) {
            stopEverything();
            return;
        }
        showForeground();
    }

    private void showForeground() {
        pushNotification(buildNotification());
        Track t = PlayerHolder.get().currentTrack();
        if (t != null && t != lastArtworkTrack) {
            lastArtworkTrack = t;
            loadArtwork(t);
        }
    }

    private void pushNotification(Notification n) {
        if (foreground) {
            getSystemService(NotificationManager.class).notify(NOTIF_ID, n);
        } else {
            startForeground(NOTIF_ID, n);
            foreground = true;
        }
    }

    /** 异步加载封面作为通知大图（本地歌无封面则跳过） */
    private void loadArtwork(Track t) {
        if (t.artworkUrl == null || t.artworkUrl.isEmpty()) return;
        Glide.with(this)
                .asBitmap()
                .load(t.artworkUrl)
                .apply(new RequestOptions()
                        .placeholder(R.drawable.ic_music_note)
                        .transform(new RoundedCorners(24)))
                .into(new CustomTarget<Bitmap>(256, 256) {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource,
                                                @Nullable Transition<? super Bitmap> transition) {
                        if (foreground && PlayerHolder.get().currentTrack() == t) {
                            pushNotification(buildNotification(resource));
                        }
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }
                });
    }

    private void stopEverything() {
        if (foreground) {
            stopForeground(STOP_FOREGROUND_REMOVE);
            foreground = false;
        }
        stopSelf();
    }

    private Notification buildNotification() {
        return buildBuilder().build();
    }

    private Notification buildNotification(Bitmap largeIcon) {
        return buildBuilder().setLargeIcon(largeIcon).build();
    }

    private NotificationCompat.Builder buildBuilder() {
        Track t = PlayerHolder.get().currentTrack();
        boolean playing = PlayerHolder.get().isPlaying();
        String title = t != null ? t.displayTitle() : "未在播放";
        String artist = t != null ? t.displayArtist() : "";

        Intent open = new Intent(this, PlayerActivity.class);
        PendingIntent openPi = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_music_note)
                .setContentTitle(title)
                .setContentText(artist)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setContentIntent(openPi)
                .addAction(new NotificationCompat.Action(R.drawable.ic_prev, "上一曲",
                        actionPi(1, ACTION_PREV)))
                .addAction(new NotificationCompat.Action(
                        playing ? R.drawable.ic_pause : R.drawable.ic_play, "播放/暂停",
                        actionPi(2, ACTION_PLAY_PAUSE)))
                .addAction(new NotificationCompat.Action(R.drawable.ic_next, "下一曲",
                        actionPi(3, ACTION_NEXT)));
    }

    private PendingIntent actionPi(int code, String action) {
        Intent i = new Intent(this, PlaybackService.class).setAction(action);
        return PendingIntent.getService(this, code, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "播放控制",
                    NotificationManager.IMPORTANCE_LOW);
            ch.setShowBadge(false);
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }

    @Override
    public void onDestroy() {
        PlayerHolder.get().removeListener(this::onPlaybackChanged);
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
