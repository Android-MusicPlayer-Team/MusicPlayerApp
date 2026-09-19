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

    /** 播放模式：0=顺序循环队列，1=单曲循环，2=随机播放 */
    public static final int MODE_SEQUENCE = 0;
    public static final int MODE_ONE = 1;
    public static final int MODE_SHUFFLE = 2;

    private final ExoPlayer player;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<Track> queue = new ArrayList<>();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private int index = -1;
    private int playMode = MODE_SEQUENCE;
    private List<Integer> shuffleOrder = new ArrayList<>();
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
        if (playMode == MODE_ONE) {
            player.seekTo(0);
            player.play();
        } else {
            next(true);
        }
    }

    public void playQueue(List<Track> tracks, int startIndex) {
        queue.clear();
        queue.addAll(tracks);
        index = Math.max(0, Math.min(startIndex, queue.size() - 1));
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

    /** index（逻辑位）→ 队列实际下标 */
    private int actualIndex() {
        if (index < 0 || index >= queue.size()) return index;
        if (playMode == MODE_SHUFFLE && index < shuffleOrder.size()) {
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
        int nextIdx;
        if (playMode == MODE_SHUFFLE) {
            if (index + 1 < shuffleOrder.size()) {
                nextIdx = index + 1;
            } else {
                nextIdx = 0; // 随机队列播完自动重新洗牌重头来
            }
        } else {
            if (index + 1 < queue.size()) {
                nextIdx = index + 1;
            } else {
                nextIdx = 0; // 顺序播完循环队列
            }
        }
        index = nextIdx;
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
            index = queue.size() - 1;
        }
        loadCurrent();
        player.play();
        notifyChanged();
    }

    /** 点击队列中某首歌切过去 */
    public void playAt(int posInQueue) {
        if (queue.isEmpty() || posInQueue < 0 || posInQueue >= queue.size()) return;
        // 随机模式下 posInQueue 是 queue 下标，需要换算成 shuffleOrder 位置
        if (playMode == MODE_SHUFFLE) {
            int p = shuffleOrder.indexOf(posInQueue);
            if (p >= 0) index = p;
        } else {
            index = posInQueue;
        }
        loadCurrent();
        player.play();
        notifyChanged();
    }

    /** 队列列表（给 UI 展示） */
    public List<Track> getQueue() {
        return new ArrayList<>(queue);
    }

    /** 当前在队列中的位置（UI 高亮用） */
    public int getQueuePosition() {
        return actualIndex();
    }

    /** 删除队列里第 pos 首，返回删除后是否还在播放 */
    public void removeAt(int pos) {
        if (pos < 0 || pos >= queue.size()) return;
        queue.remove(pos);
        if (playMode == MODE_SHUFFLE) {
            int p = shuffleOrder.indexOf(pos);
            if (p >= 0) shuffleOrder.remove(p);
            else buildShuffleOrder();
            // 重建 shuffleOrder 的值映射
            List<Integer> rebuilt = new ArrayList<>();
            for (int i = 0; i < queue.size(); i++) rebuilt.add(i);
            Collections.shuffle(rebuilt);
            shuffleOrder = rebuilt;
        }
        int curActual = actualIndex();
        if (queue.isEmpty()) {
            index = -1;
            player.stop();
        } else if (pos < curActual) {
            index--;
        } else if (pos == curActual) {
            // 删了正在放的，播下一首
            if (index >= queue.size()) index = 0;
            loadCurrent();
            player.play();
        }
        notifyChanged();
    }

    public void seekTo(long ms) {
        player.seekTo(ms);
    }

    /** 循环切换：顺序循环 → 单曲循环 → 随机播放 */
    public void cycleMode() {
        playMode = (playMode + 1) % 3;
        if (playMode == MODE_SHUFFLE) {
            int cur = actualIndex();
            buildShuffleOrder();
            int pos = shuffleOrder.indexOf(cur);
            if (pos > 0) Collections.swap(shuffleOrder, 0, pos);
            index = 0;
        }
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

    public int getPlayMode() {
        return playMode;
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
