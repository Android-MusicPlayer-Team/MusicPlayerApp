package com.redmusic.player.util;

import android.content.Context;
import android.content.SharedPreferences;

<<<<<<< HEAD
import com.redmusic.player.model.Playlist;
=======
>>>>>>> 97fccea04fcf013b06e6098484e14b10262d18bb
import com.redmusic.player.model.Track;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Prefs {
    private static SharedPreferences sp;

    public static void init(Context c) {
        sp = c.getSharedPreferences("redmusic", Context.MODE_PRIVATE);
    }

    private static String keyOf(Track t) {
        return t.title + "|" + t.artist;
    }

    public static boolean isFav(Track t) {
        return sp.getStringSet("favs", new LinkedHashSet<String>()).contains(keyOf(t));
    }

    public static void toggleFav(Track t) {
        Set<String> set = new LinkedHashSet<>(sp.getStringSet("favs", new LinkedHashSet<String>()));
        String k = keyOf(t);
        if (set.contains(k)) {
            set.remove(k);
        } else {
            set.add(k);
            sp.edit().putString("fav_" + k, trackToJson(t)).apply();
        }
        sp.edit().putStringSet("favs", set).apply();
    }

    public static List<Track> getFavs() {
        List<Track> list = new ArrayList<>();
        Set<String> set = sp.getStringSet("favs", new LinkedHashSet<String>());
        for (String k : set) {
            Track t = trackFromJson(sp.getString("fav_" + k, ""));
            if (t != null) list.add(t);
        }
        return list;
    }

    public static void addRecent(Track t) {
        Set<String> set = new LinkedHashSet<>(sp.getStringSet("recents", new LinkedHashSet<String>()));
        String k = keyOf(t);
        set.remove(k);
        set.add(k);
        sp.edit().putString("recent_" + k, trackToJson(t)).apply();
        while (set.size() > 30) {
            String oldest = set.iterator().next();
            set.remove(oldest);
        }
        sp.edit().putStringSet("recents", set).apply();
    }

    public static List<Track> getRecents() {
        List<Track> list = new ArrayList<>();
        Set<String> set = sp.getStringSet("recents", new LinkedHashSet<String>());
        for (String k : set) {
            Track t = trackFromJson(sp.getString("recent_" + k, ""));
            if (t != null) list.add(t);
        }
        // 倒序：最近的在最前
        java.util.Collections.reverse(list);
        return list;
    }

    public static void addHistory(String q) {
        Set<String> set = new LinkedHashSet<>(sp.getStringSet("history", new LinkedHashSet<String>()));
        set.remove(q);
        set.add(q);
        while (set.size() > 10) {
            String oldest = set.iterator().next();
            set.remove(oldest);
        }
        sp.edit().putStringSet("history", set).apply();
    }

    public static List<String> getHistory() {
        List<String> list = new ArrayList<>(sp.getStringSet("history", new LinkedHashSet<String>()));
        java.util.Collections.reverse(list);
        return list;
    }

    public static void clearHistory() {
        sp.edit().remove("history").apply();
    }

<<<<<<< HEAD
    // ================= 我导入的音乐 =================

    /** 保存导入的本地歌曲列表（扫描指定目录得到） */
    public static void saveImportedTracks(List<Track> list) {
        JSONArray arr = new JSONArray();
        for (Track t : list) {
            arr.put(trackToJson(t));
        }
        sp.edit().putString("imported", arr.toString()).apply();
    }

    public static List<Track> getImportedTracks() {
        List<Track> list = new ArrayList<>();
        String s = sp.getString("imported", "");
        try {
            JSONArray arr = new JSONArray(s);
            for (int i = 0; i < arr.length(); i++) {
                Track t = trackFromJson(arr.getString(i));
                if (t != null) list.add(t);
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    // ================= 我的歌单 =================

    public static void savePlaylists(List<Playlist> pls) {
        JSONArray arr = new JSONArray();
        for (Playlist p : pls) {
            try {
                JSONObject o = new JSONObject();
                o.put("id", p.id);
                o.put("name", p.name);
                JSONArray ta = new JSONArray();
                for (Track t : p.tracks) ta.put(trackToJson(t));
                o.put("tracks", ta);
                arr.put(o);
            } catch (Exception ignored) {
            }
        }
        sp.edit().putString("playlists", arr.toString()).apply();
    }

    public static List<Playlist> getPlaylists() {
        List<Playlist> list = new ArrayList<>();
        String s = sp.getString("playlists", "");
        try {
            JSONArray arr = new JSONArray(s);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Playlist p = new Playlist(o.optString("id"), o.optString("name"));
                JSONArray ta = o.optJSONArray("tracks");
                if (ta != null) {
                    for (int j = 0; j < ta.length(); j++) {
                        Track t = trackFromJson(ta.getString(j));
                        if (t != null) p.tracks.add(t);
                    }
                }
                list.add(p);
            }
        } catch (Exception ignored) {
        }
        return list;
    }

=======
>>>>>>> 97fccea04fcf013b06e6098484e14b10262d18bb
    public static String trackToJson(Track t) {
        try {
            JSONObject o = new JSONObject();
            o.put("id", t.id);
            o.put("title", t.title);
            o.put("artist", t.artist);
            o.put("album", t.album);
            o.put("art", t.artworkUrl);
            o.put("pre", t.previewUrl);
<<<<<<< HEAD
            o.put("lyr", t.lyricFile);
=======
>>>>>>> 97fccea04fcf013b06e6098484e14b10262d18bb
            o.put("dur", t.durationMs);
            return o.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public static Track trackFromJson(String json) {
        try {
            JSONObject o = new JSONObject(json);
            Track t = new Track();
            t.id = o.optString("id");
            t.title = o.optString("title");
            t.artist = o.optString("artist");
            t.album = o.optString("album");
            t.artworkUrl = o.optString("art");
            t.previewUrl = o.optString("pre");
<<<<<<< HEAD
            t.lyricFile = o.optString("lyr");
=======
>>>>>>> 97fccea04fcf013b06e6098484e14b10262d18bb
            t.durationMs = o.optLong("dur");
            return t;
        } catch (Exception e) {
            return null;
        }
    }
}
