package com.redmusic.player.util;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.Toast;

import com.redmusic.player.model.Playlist;
import com.redmusic.player.model.Track;

import java.util.List;

/** 歌单通用操作：把任意歌曲（导入的本地歌 / 搜索的网络歌）加入某个歌单 */
public class PlaylistHelper {

    /** 弹窗让用户选择一个歌单，把 t 加进去 */
    public static void addToPlaylist(Context ctx, Track t) {
        List<Playlist> pls = Prefs.getPlaylists();
        if (pls.isEmpty()) {
            Toast.makeText(ctx, "还没有歌单，请先在\"我的 - 歌单\"里新建", Toast.LENGTH_LONG).show();
            return;
        }
        String[] names = new String[pls.size()];
        for (int i = 0; i < pls.size(); i++) names[i] = pls.get(i).name;
        new AlertDialog.Builder(ctx)
                .setTitle("加入歌单：" + t.displayTitle())
                .setItems(names, (d, w) -> {
                    Playlist p = pls.get(w);
                    if (!addNoSave(p, t)) {
                        Toast.makeText(ctx, "已在歌单中", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    save(p);
                    Toast.makeText(ctx, "已加入歌单 " + p.name, Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    /** 按 id 去重后加入，返回是否真正加入 */
    public static boolean addNoSave(Playlist p, Track t) {
        for (Track x : p.tracks) {
            if (t.id != null && t.id.equals(x.id)) return false;
        }
        p.tracks.add(t);
        return true;
    }

    /** 更新某个歌单到存储 */
    public static void save(Playlist updated) {
        List<Playlist> pls = Prefs.getPlaylists();
        for (int i = 0; i < pls.size(); i++) {
            if (pls.get(i).id.equals(updated.id)) {
                pls.set(i, updated);
                break;
            }
        }
        Prefs.savePlaylists(pls);
    }
}
