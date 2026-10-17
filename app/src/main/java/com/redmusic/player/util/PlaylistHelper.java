package com.redmusic.player.util;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import com.redmusic.player.model.Playlist;
import com.redmusic.player.model.Track;

import java.util.List;
import java.util.UUID;

/** 歌单通用操作：把任意歌曲（导入的本地歌 / 搜索的网络歌）加入某个歌单 */
public class PlaylistHelper {

    /** 弹窗让用户选择一个歌单，把 t 加进去；没歌单时可在这里直接新建 */
    public static void addToPlaylist(Context ctx, Track t) {
        List<Playlist> pls = Prefs.getPlaylists();
        if (pls.isEmpty()) {
            showCreateDialog(ctx, t);
            return;
        }
        String[] names = new String[pls.size() + 1];
        for (int i = 0; i < pls.size(); i++) names[i] = pls.get(i).name;
        names[pls.size()] = "＋ 新建歌单";
        new AlertDialog.Builder(ctx)
                .setTitle("加入歌单：" + t.displayTitle())
                .setItems(names, (d, w) -> {
                    if (w == pls.size()) {
                        showCreateDialog(ctx, t);
                        return;
                    }
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

    /** 新建歌单并直接把当前歌曲加入 */
    private static void showCreateDialog(Context ctx, Track t) {
        EditText input = new EditText(ctx);
        input.setHint("歌单名称");
        input.setSingleLine(true);
        final AlertDialog d = new AlertDialog.Builder(ctx)
                .setTitle("新建歌单")
                .setView(input)
                .setPositiveButton("创建并加入", (dlg, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(ctx, "请输入歌单名称", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Playlist np = new Playlist(UUID.randomUUID().toString(), name);
                    np.tracks.add(t);
                    List<Playlist> pls = Prefs.getPlaylists();
                    pls.add(np);
                    Prefs.savePlaylists(pls);
                    Toast.makeText(ctx, "已创建歌单 " + name + " 并加入歌曲", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .create();
        d.show();
        int pc = com.redmusic.player.util.ThemeManager.primaryColor(ctx);
        d.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(pc);
        d.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(pc);
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
