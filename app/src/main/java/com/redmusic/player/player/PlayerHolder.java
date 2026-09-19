package com.redmusic.player.player;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;

import com.redmusic.player.model.Track;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class PlayerHolder {
    private static PlayerHolder sInstance;

    private final ExoPlayer player;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<Track> queue = new ArrayList<>();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private int index = -1;
    private boolean shuffle = false;
    private List<Integer> shuffleOrder = new ArrayList<>();
    private int repeatMode = Player.REPEAT_MODE_OFF;
    private float speed = 1f;

    public static synchronized void init(Context ctx) {
        if (sInstance == null) {
            sInstance = new PlayerHolder(ctx.getApplicationContext());
        }
    }

    public static PlayerHolder get() {
        return sInstance;
    }

    private PlayerHolder(Context ctx) {
        player = new ExoPlayer.Builder(ctx).build();
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_ENDED) {
                    onTrackEnded();
                }
                notifyChanged();
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                notifyChanged();
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                notifyChanged();
            }

            @Override
            public void onMediaItemTransition(MediaItem mediaItem, int reason) {
                notifyChanged();
            }
        });
    }

    private void onTrackEnded() {
        if (repeatMode == Player.REPEAT_MODE_ONE) {
            player.seekTo(0);
            player.play();
        } else {
            next(true);
        }
    }

    public void playQueue(List<Track> tracks, int startIndex) {
        queue.clear();
        queue.addAll(tracks);
        index = Math.max(0, startIndex);
        buildShuffleOrder();
        loadCurrent();
        player.play();
        notifyChanged();
    }

    private void buildShuffleOrder() {
        shuffleOrder.clear();
        for (int i = 0; i < queue.size(); i++) {
            shuffleOrder.add(i);
        }
        Collections.shuffle(shuffleOrder);
    }

    private int actualIndex() {
        if (index < 0 || index >= queue.size()) return index;
        if (shuffle && index < shuffleOrder.size()) {
            return shuffleOrder.get(index);
        }
        return index;
    }

    private void loadCurrent() {
        if (queue.isEmpty() || index < 0 || index >= queue.size()) return;
        Track t = queue.get(actualIndex());
        if (t == null) return;
        String url = t.previewUrl;
        if (url == null || url.isEmpty()) {
            url = "https://audio-ssl.itunes.apple.com/itunes-assets/placeholder.m4a";
        }
        player.setMediaItem(MediaItem.fromUri(url));
        player.prepare();
    }

    public void playPause() {
        if (queue.isEmpty()) return;
        if (player.isPlaying()) {
            player.pause();
        } else {
            player.play();
        }
        notifyChanged();
    }

    public void next(boolean auto) {
        if (queue.isEmpty()) return;
        if (shuffle) {
            if (index + 1 < shuffleOrder.size()) {
                index++;
            } else if (repeatMode == Player.REPEAT_MODE_ALL) {
                index = 0;
            } else {
                if (auto) {
                    stopAndClear();
                    return;
                }
                index = 0;
            }
        } else {
            if (index + 1 < queue.size()) {
                index++;
            } else if (repeatMode == Player.REPEAT_MODE_ALL) {
                index = 0;
            } else {
                if (auto) {
                    stopAndClear();
                    return;
                }
                index = 0;
            }
        }
        loadCurrent();
        player.play();
        notifyChanged();
    }

    public void prev() {
        if (queue.isEmpty()) return;
        if (player.getCurrentPosition() > 3000) {
            player.seekTo(0);
            return;
        }
        if (index > 0) {
            index--;
        } else {
            index = 0;
        }
        loadCurrent();
        player.play();
        notifyChanged();
    }

    private void stopAndClear() {
        index = -1;
        player.stop();
        notifyChanged();
    }

    public void seekTo(long ms) {
        player.seekTo(ms);
    }

    public void toggleShuffle() {
        shuffle = !shuffle;
        if (shuffle) {
            int cur = actualIndex();
            buildShuffleOrder();
            // 让当前歌曲保持在播放位置附近
            int pos = shuffleOrder.indexOf(cur);
            if (pos > 0) {
                Collections.swap(shuffleOrder, 0, pos);
            }
        }
        notifyChanged();
    }

    public void cycleRepeat() {
        repeatMode = (repeatMode + 1) % 3;
        player.setRepeatMode(repeatMode);
        notifyChanged();
    }

    public void setSpeed(float s) {
        speed = s;
        player.setPlaybackSpeed(s);
        notifyChanged();
    }

    public float getSpeed() {
        return speed;
    }

    public int getRepeatMode() {
        return repeatMode;
    }

    public boolean isShuffle() {
        return shuffle;
    }

    public boolean isPlaying() {
        return player.isPlaying();
    }

    public boolean hasTrack() {
        return currentTrack() != null;
    }

    public long getPosition() {
        return player.getCurrentPosition();
    }

    public long getDuration() {
        long d = player.getDuration();
        if (d > 0) return d;
        Track t = currentTrack();
        return t != null ? t.durationMs : 0;
    }

    public Track currentTrack() {
        if (index < 0 || queue.isEmpty() || index >= queue.size()) return null;
        return queue.get(actualIndex());
    }

    public int getAudioSessionId() {
        return player.getAudioSessionId();
    }

    public ExoPlayer getPlayer() {
        return player;
    }

    public void addListener(Runnable r) {
        listeners.add(r);
    }

    public void removeListener(Runnable r) {
        listeners.remove(r);
    }

    public void release() {
        player.release();
    }

    public void notifyChanged() {
        for (Runnable r : listeners) {
            main.post(r);
        }
    }
}
