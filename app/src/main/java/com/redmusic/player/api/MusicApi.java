package com.redmusic.player.api;

import android.net.Uri;

import com.redmusic.player.model.LyricLine;
import com.redmusic.player.model.Track;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 网易云音乐 API（NeteaseCloudMusicApi）客户端
 *
 * 歌曲搜索、播放地址、歌词都用【同一个歌曲 id】查询，
 * 出自同一个数据库，歌词时间轴与歌曲天然对齐（这就是同步的关键）。
 *
 * BASE 地址：
 *  - 模拟器调试：https://api.2leo.top  （10.0.2.2 是模拟器访问电脑本机的固定地址）
 *  - 真机调试：改成电脑的局域网 IP，例如 http://192.168.1.100:3000 （手机与电脑同一 WiFi）
 */
public class MusicApi {

    public static final String BASE = "https://api.2leo.top";

    public interface TrackListCallback {
        void onSuccess(List<Track> tracks);

        void onError(String msg);
    }

    public interface LyricCallback {
        void onSuccess(List<LyricLine> lines);

        void onError(String msg);
    }

    public interface SuggestCallback {
        void onResult(List<String> words);
    }

    /** 输入时实时联想词（百度 suggestion 接口，无需鉴权） */
    public static void suggest(String keyword, SuggestCallback cb) {
        String url = "https://suggestion.baidu.com/su?wd=" + Uri.encode(keyword.trim()) + "&p=3&cb=";
        ApiClient.get(url, new ApiClient.Callback() {
            @Override
            public void onResult(String body) {
                List<String> words = new ArrayList<>();
                try {
                    java.util.regex.Matcher m = java.util.regex.Pattern
                            .compile("s:\\[([^\\]]*)\\]").matcher(body);
                    if (m.find()) {
                        String arr = m.group(1);
                        java.util.regex.Matcher sm = java.util.regex.Pattern
                                .compile("\"([^\"]*)\"").matcher(arr);
                        while (sm.find()) {
                            if (!sm.group(1).isEmpty()) words.add(sm.group(1));
                        }
                    }
                } catch (Exception ignored) {
                }
                cb.onResult(words);
            }

            @Override
            public void onError(String msg) {
                cb.onResult(new ArrayList<>());
            }
        });
    }

    /** 解析网易云 song 对象为 Track（搜索结果 / 新歌推荐共用） */
    private static Track parseSong(JSONObject o) {
        Track t = new Track();
        t.id = String.valueOf(o.optLong("id", 0));
        t.title = o.optString("name", "未知歌曲");
        JSONArray artists = o.optJSONArray("artists");
        if (artists != null && artists.length() > 0) {
            try {
                t.artist = artists.getJSONObject(0).optString("name", "未知歌手");
            } catch (Exception e) {
                t.artist = "未知歌手";
            }
        } else {
            t.artist = "未知歌手";
        }
        JSONObject album = o.optJSONObject("album");
        if (album != null) {
            t.album = album.optString("name", "");
            t.artworkUrl = album.optString("picUrl", "");
        }
        t.durationMs = o.optLong("duration", 0);
        t.previewUrl = ""; // 播放前再用 resolveUrl 换取真实地址
        return t;
    }

    /** 网易云关键词搜索 */
    public static void search(String term, TrackListCallback cb) {
        String url = BASE + "/search?keywords=" + Uri.encode(term.trim()) + "&limit=30";
        ApiClient.get(url, new ApiClient.Callback() {
            @Override
            public void onResult(String body) {
                List<Track> list = new ArrayList<>();
                try {
                    JSONObject root = new JSONObject(body);
                    JSONObject result = root.optJSONObject("result");
                    JSONArray songs = result != null ? result.optJSONArray("songs") : null;
                    if (songs != null) {
                        for (int i = 0; i < songs.length(); i++) {
                            list.add(parseSong(songs.getJSONObject(i)));
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

    /** 新歌推荐（首页热榜位） */
    public static void topSongs(TrackListCallback cb) {
        ApiClient.get(BASE + "/personalized/newsong", new ApiClient.Callback() {
            @Override
            public void onResult(String body) {
                List<Track> list = new ArrayList<>();
                try {
                    JSONObject root = new JSONObject(body);
                    JSONArray data = root.optJSONArray("result");
                    if (data != null) {
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject wrapper = data.getJSONObject(i);
                            JSONObject song = wrapper.optJSONObject("song");
                            if (song != null) {
                                Track t = parseSong(song);
                                t.rank = i + 1;
                                list.add(t);
                            }
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

    /** 用歌曲 id 换取真实播放地址（网易云 /song/url） */
    public static void resolveUrl(Track t, TrackListCallback cb) {
        String url = BASE + "/song/url/v1?id=" + t.id + "&level=standard";
        ApiClient.get(url, new ApiClient.Callback() {
            @Override
            public void onResult(String body) {
                try {
                    JSONObject root = new JSONObject(body);
                    JSONArray data = root.optJSONArray("data");
                    if (data != null && data.length() > 0) {
                        String u = data.getJSONObject(0).optString("url", "");
                        if (u != null && !u.isEmpty()) t.previewUrl = u;
                    }
                } catch (Exception ignored) {
                }
                List<Track> one = new ArrayList<>();
                one.add(t);
                cb.onSuccess(one); // 无论成败都回调，让播放继续
            }

            @Override
            public void onError(String msg) {
                List<Track> one = new ArrayList<>();
                one.add(t);
                cb.onSuccess(one);
            }
        });
    }

    /** 按歌曲 id 从网易云取同步歌词（与播放歌曲同源） */
    public static void loadLyrics(Track t, LyricCallback cb) {
        String url = BASE + "/lyric?id=" + t.id;
        ApiClient.get(url, new ApiClient.Callback() {
            @Override
            public void onResult(String body) {
                List<LyricLine> lines = new ArrayList<>();
                try {
                    JSONObject root = new JSONObject(body);
                    JSONObject lrc = root.optJSONObject("lrc");
                    String synced = lrc != null ? lrc.optString("lyric", "") : "";
                    if (synced != null && !synced.isEmpty()) {
                        lines = LyricLine.parse(synced);
                    }
                } catch (Exception ignored) {
                }
                if (lines.isEmpty()) {
                    cb.onError("暂无歌词");
                } else {
                    cb.onSuccess(lines);
                }
            }

            @Override
            public void onError(String msg) {
                cb.onError(msg);
            }
        });
    }
}
