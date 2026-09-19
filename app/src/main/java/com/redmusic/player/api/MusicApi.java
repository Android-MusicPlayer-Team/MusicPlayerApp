package com.redmusic.player.api;

import android.net.Uri;

import com.redmusic.player.model.LyricLine;
import com.redmusic.player.model.Track;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MusicApi {

    public static final String ITUNES_SEARCH = "https://itunes.apple.com/search?media=music&entity=song&limit=30&term=";
    public static final String ITUNES_TOP = "https://itunes.apple.com/us/rss/topsongs/limit=50/json";
    public static final String LRCLIB = "https://lrclib.net/api/search";

    public interface TrackListCallback {
        void onSuccess(List<Track> tracks);

        void onError(String msg);
    }

    public interface LyricCallback {
        void onSuccess(List<LyricLine> lines);

        void onError(String msg);
    }

    /** iTunes 关键词搜索 */
    public static void search(String term, TrackListCallback cb) {
        String url = ITUNES_SEARCH + Uri.encode(term.trim());
        ApiClient.get(url, new ApiClient.Callback() {
            @Override
            public void onResult(String body) {
                List<Track> list = new ArrayList<>();
                try {
                    JSONObject root = new JSONObject(body);
                    JSONArray results = root.optJSONArray("results");
                    if (results != null) {
                        for (int i = 0; i < results.length(); i++) {
                            JSONObject o = results.getJSONObject(i);
                            Track t = new Track();
                            t.id = String.valueOf(o.optLong("trackId", i));
                            t.title = o.optString("trackName", "未知歌曲");
                            t.artist = o.optString("artistName", "未知歌手");
                            t.album = o.optString("collectionName", "");
                            String art = o.optString("artworkUrl100", "");
                            t.artworkUrl = art.isEmpty() ? "" : art.replace("100x100", "400x400");
                            t.previewUrl = o.optString("previewUrl", "");
                            t.durationMs = o.optLong("trackTimeMillis", 0);
                            t.genre = o.optString("primaryGenreName", "");
                            list.add(t);
                        }
                    }
                } catch (Exception ignored) {
                }
                cb.onSuccess(list);
            }

            @Override
            public void onError(String msg) {
                cb.onError(msg);
            }
        });
    }

    /** iTunes 美国区热门歌曲榜 Top 50 */
    public static void topSongs(TrackListCallback cb) {
        ApiClient.get(ITUNES_TOP, new ApiClient.Callback() {
            @Override
            public void onResult(String body) {
                List<Track> list = new ArrayList<>();
                try {
                    JSONObject root = new JSONObject(body);
                    JSONObject feed = root.optJSONObject("feed");
                    JSONArray entries = feed != null ? feed.optJSONArray("entry") : null;
                    if (entries != null) {
                        for (int i = 0; i < entries.length(); i++) {
                            JSONObject e = entries.getJSONObject(i);
                            Track t = new Track();
                            t.id = "top_" + i;
                            t.title = e.optJSONObject("im:name") != null
                                    ? e.optJSONObject("im:name").optString("label", "未知歌曲")
                                    : "未知歌曲";
                            t.artist = e.optJSONObject("im:artist") != null
                                    ? e.optJSONObject("im:artist").optString("label", "未知歌手")
                                    : "未知歌手";
                            t.album = e.optJSONObject("im:collection") != null
                                    ? e.optJSONObject("im:collection").optJSONObject("im:name").optString("label", "")
                                    : "";
                            JSONArray images = e.optJSONArray("im:image");
                            if (images != null && images.length() > 0) {
                                t.artworkUrl = images.getJSONObject(images.length() - 1).optString("label", "");
                            }
                            t.previewUrl = ""; // RSS 榜无预览，播放时按歌名解析
                            t.genre = "";
                            t.durationMs = 0;
                            t.rank = i + 1;
                            list.add(t);
                        }
                    }
                } catch (Exception ignored) {
                }
                cb.onSuccess(list);
            }

            @Override
            public void onError(String msg) {
                cb.onError(msg);
            }
        });
    }

    /** 解析排行榜歌曲的试听地址（用歌名+歌手回查 iTunes） */
    public static void resolvePreview(Track t, TrackListCallback cb) {
        String term = t.title + " " + t.artist;
        String url = ITUNES_SEARCH + Uri.encode(term.trim());
        ApiClient.get(url, new ApiClient.Callback() {
            @Override
            public void onResult(String body) {
                try {
                    JSONObject root = new JSONObject(body);
                    JSONArray results = root.optJSONArray("results");
                    if (results != null && results.length() > 0) {
                        JSONObject o = results.getJSONObject(0);
                        t.previewUrl = o.optString("previewUrl", "");
                        if (t.artworkUrl == null || t.artworkUrl.isEmpty()) {
                            String art = o.optString("artworkUrl100", "");
                            t.artworkUrl = art.isEmpty() ? "" : art.replace("100x100", "400x400");
                        }
                        t.durationMs = o.optLong("trackTimeMillis", 0);
                    }
                } catch (Exception ignored) {
                }
                List<Track> one = new ArrayList<>();
                one.add(t);
                cb.onSuccess(one);
            }

            @Override
            public void onError(String msg) {
                cb.onError(msg);
            }
        });
    }

    /** 从 lrclib 获取歌词 */
    public static void loadLyrics(Track t, LyricCallback cb) {
        String url = LRCLIB + "?track_name=" + Uri.encode(t.title)
                + "&artist_name=" + Uri.encode(t.artist)
                + "&album_name=" + Uri.encode(t.album);
        ApiClient.get(url, new ApiClient.Callback() {
            @Override
            public void onResult(String body) {
                List<LyricLine> lines = new ArrayList<>();
                try {
                    JSONArray arr = new JSONArray(body);
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject o = arr.getJSONObject(i);
                        String synced = o.optString("syncedLyrics", "");
                        if (synced != null && !synced.isEmpty()) {
                            lines = LyricLine.parse(synced);
                            break;
                        }
                    }
                    if (lines.isEmpty()) {
                        cb.onError("暂无歌词");
                    } else {
                        cb.onSuccess(lines);
                    }
                } catch (Exception e) {
                    cb.onError("歌词解析失败");
                }
            }

            @Override
            public void onError(String msg) {
                cb.onError(msg);
            }
        });
    }
}
