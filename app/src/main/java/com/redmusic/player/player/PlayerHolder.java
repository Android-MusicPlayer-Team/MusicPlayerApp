package com.redmusic.player.player;

import android.content.Context;
<<<<<<< HEAD
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import androidx.core.content.ContextCompat;

=======
import android.os.Handler;
import android.os.Looper;

>>>>>>> 97fccea04fcf013b06e6098484e14b10262d18bb
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;

import com.redmusic.player.api.MusicApi;
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
<<<<<<< HEAD
    private final Context appContext;
=======
>>>>>>> 97fccea04fcf013b06e6098484e14b10262d18bb
    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<Track> queue = new ArrayList<>();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private int index = -1;
    private int playMode = MODE_SEQUENCE;
    private List<Integer> shuffleOrder = new ArrayList<>();
    private float speed = 1f;

<<<<<<< HEAD
    /** 连续播放失败计数（用于整单无法播放判定） */
    private int consecutiveFailures = 0;
    /** 整单都无法播放（如完全无网络） */
    private boolean allFailed = false;

=======
>>>>>>> 97fccea04fcf013b06e6098484e14b10262d18bb
    public static synchronized void init(Context ctx) {
        if (sInstance == null) {
            sInstance = new PlayerHolder(ctx.getApplicationContext());
        }
    }

    public static PlayerHolder get() {
        return sInstance;
    }

    private PlayerHolder(Context ctx) {
<<<<<<< HEAD
        appContext = ctx.getApplicationContext();
=======
>>>>>>> 97fccea04fcf013b06e6098484e14b10262d18bb
        player = new ExoPlayer.Builder(ctx).build();
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
<<<<<<< HEAD
                if (state == Player.STATE_READY) {
                    // 能正常播放了，重置失败计数
                    consecutiveFailures = 0;
                    allFailed = false;
                }
=======
>>>>>>> 97fccea04fcf013b06e6098484e14b10262d18bb
                if (state == Player.STATE_ENDED) {
                    onTrackEnded();
                }
                notifyChanged();
            }

            @Override
            public void onPlayerError(PlaybackException error) {
<<<<<<< HEAD
                // 播放失败（如无网络）→ 自动跳下一首；整单都失败则标记
                handlePlaybackFailure();
=======
>>>>>>> 97fccea04fcf013b06e6098484e14b10262d18bb
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
<<<<<<< HEAD
        consecutiveFailures = 0;
        allFailed = false;
        queue.clear();
        queue.addAll(tracks);
        int start = Math.max(0, Math.min(startIndex, queue.size() - 1));
        buildShuffleOrder();
        if (playMode == MODE_SHUFFLE) {
            // 随机模式下把用户点的那首排到随机队列首位，先播它，其余随机
            int p = shuffleOrder.indexOf(start);
            if (p > 0) {
                shuffleOrder.remove(p);
                shuffleOrder.add(0, start);
            }
            index = 0;
        } else {
            index = start;
        }
        loadCurrent();
        player.play();
        // 启动前台服务，后台/锁屏也能继续播放并有通知栏控制
        if (appContext != null) {
            ContextCompat.startForegroundService(appContext,
                    new Intent(appContext, PlaybackService.class));
        }
=======
        queue.clear();
        queue.addAll(tracks);
        index = Math.max(0, Math.min(startIndex, queue.size() - 1));
        buildShuffleOrder();
        loadCurrent();
        player.play();
>>>>>>> 97fccea04fcf013b06e6098484e14b10262d18bb
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
        if (t.previewUrl == null || t.previewUrl.isEmpty()) {
            // 网易云歌曲：先异步换取真实播放地址，再开始播放
            MusicApi.resolveUrl(t, new MusicApi.TrackListCallback() {
                @Override
                public void onSuccess(List<Track> tracks) {
                    main.post(() -> {
                        // 切歌竞态防护：回调回来时若已切到别的歌，就不覆盖
                        if (currentTrack() == t) startPlay(t);
                    });
                }

                @Override
                public void onError(String msg) {
                    main.post(() -> {
<<<<<<< HEAD
                        if (currentTrack() == t) {
                            // 在线歌曲无法取到地址（多半无网络）→ 自动跳下一首
                            handlePlaybackFailure();
                            notifyChanged();
                        }
=======
                        if (currentTrack() == t) startPlay(t);
>>>>>>> 97fccea04fcf013b06e6098484e14b10262d18bb
                    });
                }
            });
        } else {
            startPlay(t);
        }
    }

    private void startPlay(Track t) {
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

    /** 循环切换：顺序循环 → 单曲循环 → 随机播放（只改模式，不切歌） */
    public void cycleMode() {
        int curQueueIdx = actualIndex();  // 记住当前播放的歌在 queue 中的实际下标
        playMode = (playMode + 1) % 3;
        if (playMode == MODE_SHUFFLE) {
            // 切到随机：重建随机队列，把当前歌放到最前面
            buildShuffleOrder();
            int pos = shuffleOrder.indexOf(curQueueIdx);
            if (pos >= 0) {
                index = pos;
            }
        } else {
            // 从随机切回顺序/单曲：index 直接用 queue 实际下标，保持当前歌不变
            index = curQueueIdx;
        }
        notifyChanged();
    }

    public void setSpeed(float s) {
        speed = s;
        player.setPlaybackSpeed(s);
        notifyChanged();
    }

<<<<<<< HEAD
    /** 单曲循环播放失败后最多重试次数，仍失败再停止/提示 */
    private static final int ONE_MODE_RETRY_LIMIT = 3;

    /** 播放失败处理：单曲循环先重试当前歌；其它模式自动跳到下一首；整单无法播放则标记停止 */
    private void handlePlaybackFailure() {
        consecutiveFailures++;
        // 单曲循环：失败先重试当前歌（重新取地址/从头重播），避免卡死或网络抖动就直接跳走
        if (playMode == MODE_ONE && consecutiveFailures < ONE_MODE_RETRY_LIMIT) {
            loadCurrent();
            player.play();
            return;
        }
        if (queue.size() > 0 && consecutiveFailures >= queue.size()) {
            // 整单都放不了（多半无网络）→ 停止并标记
            allFailed = true;
            index = -1;
            player.stop();
            return;
        }
        next(false);
    }

    /** 整单都无法播放（如完全无网络） */
    public boolean isAllFailed() {
        return allFailed;
    }

=======
>>>>>>> 97fccea04fcf013b06e6098484e14b10262d18bb
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
