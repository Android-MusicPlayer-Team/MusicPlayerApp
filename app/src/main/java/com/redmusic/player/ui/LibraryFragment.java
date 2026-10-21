package com.redmusic.player.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.redmusic.player.R;
import com.redmusic.player.api.MusicApi;
import com.redmusic.player.model.Playlist;
import com.redmusic.player.model.Track;
import com.redmusic.player.player.PlayerHolder;
import com.redmusic.player.util.PlaylistHelper;
import com.redmusic.player.util.Prefs;
import com.redmusic.player.util.ThemeManager;

import java.io.File;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 我的音乐页：收藏 / 最近播放 / 导入（扫描本地目录）/ 我的歌单（持久） */
public class LibraryFragment extends Fragment {

    private static final int TAB_FAV = 0;
    private static final int TAB_RECENT = 1;
    private static final int TAB_IMPORT = 2;
    private static final int TAB_PLAYLISTS = 3;

    private static final String[] AUDIO_EXTS = {
            "mp3", "m4a", "aac", "flac", "wav", "ogg", "opus", "wma", "ape", "amr", "mid", "midi"
    };

    private final Handler main = new Handler(Looper.getMainLooper());
    private TrackAdapter trackAdapter;
    private PlaylistListAdapter playlistAdapter;
    private TextView tabFav, tabRecent, tabImport, tabPlaylists;
    private TextView actionScan, actionNew, actionAdd, actionDel, actionBack;
    private TextView emptyTip;
    private RecyclerView list;

    private int currentTab = TAB_FAV;
    private boolean inPlaylistDetail = false;
    private String openPlaylistId = null;

    private File browseFolder;
    private final Deque<File> browseStack = new ArrayDeque<>();
    private final List<File> browseChildren = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_library, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        tabFav = view.findViewById(R.id.tab_fav);
        tabRecent = view.findViewById(R.id.tab_recent);
        tabImport = view.findViewById(R.id.tab_import);
        tabPlaylists = view.findViewById(R.id.tab_playlists);
        actionScan = view.findViewById(R.id.action_scan);
        actionNew = view.findViewById(R.id.action_new);
        actionAdd = view.findViewById(R.id.action_add);
        actionDel = view.findViewById(R.id.action_del);
        actionBack = view.findViewById(R.id.action_back);
        emptyTip = view.findViewById(R.id.lib_empty);
        list = view.findViewById(R.id.lib_list);

        // 标题与操作按钮跟随皮肤主色
        int primary = ThemeManager.primaryColor(requireContext());
        ((TextView) view.findViewById(R.id.library_title)).setTextColor(primary);
        actionScan.setTextColor(primary);
        actionNew.setTextColor(primary);
        actionAdd.setTextColor(primary);
        actionDel.setTextColor(primary);
        actionBack.setTextColor(primary);

        // 右上角设置图标 → 打开设置页；颜色由 ic_settings 的 text_primary 资源随黑白自动切换
        view.findViewById(R.id.library_settings_btn).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SettingsActivity.class)));

        trackAdapter = new TrackAdapter(false, true);
        trackAdapter.setAction(new TrackAdapter.OnAction() {
            @Override
            public void onPlay(int position) {
                List<Track> cur = trackAdapter.getTracks();
                if (cur.isEmpty()) return;
                PlayerHolder.get().playQueue(cur, position);
                Prefs.addRecent(cur.get(position));
                startActivity(new Intent(getContext(), PlayerActivity.class));
            }

            @Override
            public void onFav(Track t) {
                Prefs.toggleFav(t);
                refresh();
                Toast.makeText(getContext(), Prefs.isFav(t) ? "已收藏" : "已取消收藏", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLongPress(Track t) {
                // 导入/收藏/最近播放里的歌长按 → 加入歌单（含网络歌曲）
                if (currentTab == TAB_IMPORT || currentTab == TAB_FAV || currentTab == TAB_RECENT) {
                    if (getContext() != null) PlaylistHelper.addToPlaylist(getContext(), t);
                } else if (currentTab == TAB_PLAYLISTS && inPlaylistDetail) {
                    showRemoveFromOpenPlaylist(t);           // 歌单详情长按 → 移除
                }
            }

            @Override
            public void onRemove(Track t) {
                // 歌单详情行内"删除"按钮 → 移除
                if (currentTab == TAB_PLAYLISTS && inPlaylistDetail) {
                    showRemoveFromOpenPlaylist(t);
                }
            }
        });

        playlistAdapter = new PlaylistListAdapter();
        playlistAdapter.setAction(this::openPlaylistDetail);

        list.setLayoutManager(new LinearLayoutManager(getContext()));

        tabFav.setOnClickListener(v -> setTab(TAB_FAV));
        tabRecent.setOnClickListener(v -> setTab(TAB_RECENT));
        tabImport.setOnClickListener(v -> setTab(TAB_IMPORT));
        tabPlaylists.setOnClickListener(v -> setTab(TAB_PLAYLISTS));
        actionScan.setOnClickListener(v -> startFolderBrowser());
        actionNew.setOnClickListener(v -> showCreatePlaylist());
        actionAdd.setOnClickListener(v -> showAddSongsToOpen());
        actionDel.setOnClickListener(v -> deleteOpenPlaylist());
        actionBack.setOnClickListener(v -> {
            if (currentTab == TAB_PLAYLISTS && inPlaylistDetail) {
                backToPlaylists();
            }
        });

        setTab(TAB_FAV);
        refresh();
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    private void setTab(int tab) {
        currentTab = tab;
        updateTabColors();
        refresh();
    }

    private void updateTabColors() {
        int red = ThemeManager.primaryColor(requireContext());
        int gray = 0xFFAAAAAA;
        tabFav.setTextColor(currentTab == TAB_FAV ? red : gray);
        tabRecent.setTextColor(currentTab == TAB_RECENT ? red : gray);
        tabImport.setTextColor(currentTab == TAB_IMPORT ? red : gray);
        tabPlaylists.setTextColor(currentTab == TAB_PLAYLISTS ? red : gray);
    }

    private void refresh() {
        if (currentTab == TAB_PLAYLISTS && inPlaylistDetail && findPlaylist(openPlaylistId) == null) {
            inPlaylistDetail = false;
            openPlaylistId = null;
        }
        if (currentTab == TAB_FAV) {
            List<Track> favs = Prefs.getFavs();
            bindTracks(favs, "还没有收藏的歌曲\n在歌曲列表点 ♥ 收藏");
            showActions(false, false, false, false, false);
        } else if (currentTab == TAB_RECENT) {
            List<Track> recents = Prefs.getRecents();
            bindTracks(recents, "还没有播放记录");
            showActions(false, false, false, false, false);
        } else if (currentTab == TAB_IMPORT) {
            List<Track> imported = Prefs.getImportedTracks();
            bindTracks(imported, "还没有导入的歌曲\n点\"扫描目录\"选择文件夹");
            showActions(true, false, false, false, false);
        } else if (currentTab == TAB_PLAYLISTS) {
            if (inPlaylistDetail) {
                Playlist p = findPlaylist(openPlaylistId);
                if (p == null) return;
                bindTracks(p.tracks, "歌单还没有歌曲\n点\"添加歌曲\"从导入里加，长按可移除");
                actionBack.setText("← 返回");
                showActions(false, false, true, true, true);
            } else {
                List<Playlist> pls = Prefs.getPlaylists();
                list.setAdapter(playlistAdapter);
                playlistAdapter.setItems(pls);
                emptyTip.setVisibility(pls.isEmpty() ? View.VISIBLE : View.GONE);
                emptyTip.setText(pls.isEmpty() ? "还没有歌单\n点\"新建歌单\"创建" : "");
                showActions(false, true, false, false, false);
            }
        }
    }

    /** 用歌曲列表填充列表（收藏/最近/导入/歌单详情共用） */
    private void bindTracks(List<Track> tracks, String emptyText) {
        list.setAdapter(trackAdapter);
        // 仅歌单详情显示行内"删除"按钮
        trackAdapter.setRemoveMode(currentTab == TAB_PLAYLISTS && inPlaylistDetail);
        trackAdapter.setTracks(tracks);
        emptyTip.setVisibility(tracks.isEmpty() ? View.VISIBLE : View.GONE);
        emptyTip.setText(tracks.isEmpty() ? emptyText : "");
    }

    private void showActions(boolean scan, boolean nw, boolean add, boolean del, boolean back) {
        actionScan.setVisibility(scan ? View.VISIBLE : View.GONE);
        actionNew.setVisibility(nw ? View.VISIBLE : View.GONE);
        actionAdd.setVisibility(add ? View.VISIBLE : View.GONE);
        actionDel.setVisibility(del ? View.VISIBLE : View.GONE);
        actionBack.setVisibility(back ? View.VISIBLE : View.GONE);
    }

    // ================= 扫描本地目录（App 内置目录浏览器 + 所有文件访问权限） =================

    /** 点"扫描目录"：先保证"所有文件访问"权限，再打开 App 内置目录浏览器 */
    private void startFolderBrowser() {
        if (getContext() == null) return;
        if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:" + getContext().getPackageName())));
            } catch (Exception e) {
                startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
            }
            Toast.makeText(getContext(), "请开启\"允许访问所有文件\"权限，再点\"扫描目录\"", Toast.LENGTH_LONG).show();
            return;
        }
        showFolderBrowser();
    }

    /** App 内置目录浏览器：选择要扫描的文件夹 */
    private void showFolderBrowser() {
        browseStack.clear();
        browseFolder = Environment.getExternalStorageDirectory();
        LinearLayout content = new LinearLayout(getContext());
        content.setOrientation(LinearLayout.VERTICAL);
        TextView path = new TextView(getContext());
        path.setTextSize(13);
        path.setTextColor(0xFFAAAAAA);
        path.setPadding(dp(18), dp(10), dp(18), dp(6));
        ListView lv = new ListView(getContext());
        lv.setDividerHeight(0);
        content.addView(path);
        content.addView(lv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(300)));

        final AlertDialog dlg = new AlertDialog.Builder(getContext())
                .setTitle("选择要扫描的文件夹")
                .setView(content)
                .setPositiveButton("扫描此目录", (d, w) -> scanFolder(browseFolder))
                .setNegativeButton("取消", null)
                .create();

        final ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_list_item_1);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener((p, v, pos, id) -> {
            boolean hasUp = !browseStack.isEmpty();
            if (hasUp && pos == 0) {
                browseFolder = browseStack.pop();      // 返回上级
            } else {
                int idx = pos - (hasUp ? 1 : 0);
                if (idx >= 0 && idx < browseChildren.size()) {
                    browseStack.push(browseFolder);
                    browseFolder = browseChildren.get(idx);
                }
            }
            refreshFolderList(path, adapter);
        });
        refreshFolderList(path, adapter);
        dlg.show();
        int pc = com.redmusic.player.util.ThemeManager.primaryColor(getContext());
        dlg.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(pc);
        dlg.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(pc);
    }

    private void refreshFolderList(TextView path, ArrayAdapter<String> adapter) {
        File[] subs = browseFolder.listFiles(File::isDirectory);
        browseChildren.clear();
        List<String> names = new ArrayList<>();
        if (!browseStack.isEmpty()) names.add("… 返回上级");
        if (subs != null) {
            for (File f : subs) {
                String n = f.getName();
                if (n != null && !n.startsWith(".")) {
                    browseChildren.add(f);
                    names.add(n);
                }
            }
        }
        adapter.clear();
        adapter.addAll(names);
        path.setText(browseFolder.getAbsolutePath());
    }

    /** 递归扫描文件夹下所有音频，持久化到导入列表 */
    private void scanFolder(File dir) {
        if (dir == null || !dir.exists() || !dir.canRead()) {
            Toast.makeText(getContext(), "无法读取该文件夹", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(getContext(), "扫描中...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            List<Track> found = new ArrayList<>();
            scanDirFiles(dir, found);
            List<Track> existing = new ArrayList<>(Prefs.getImportedTracks());
            Set<String> keys = new HashSet<>();
            for (Track t : existing) keys.add(t.id);
            int added = 0;
            for (Track t : found) {
                if (keys.add(t.id)) {
                    existing.add(t);
                    added++;
                }
            }
            Prefs.saveImportedTracks(existing);
            final int fAdded = added, fTotal = existing.size();
            main.post(() -> {
                Toast.makeText(getContext(), "扫描完成，新增 " + fAdded + " 首，共 " + fTotal + " 首",
                        Toast.LENGTH_LONG).show();
                setTab(TAB_IMPORT);
            });
        }).start();
    }

    private void scanDirFiles(File dir, List<Track> out) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                scanDirFiles(f, out);
            } else if (isAudioFile(f.getName())) {
                Track t = new Track();
                t.id = Uri.fromFile(f).toString();
                t.previewUrl = Uri.fromFile(f).toString();
                t.title = readLocalTitle(f);
                t.artist = "本地音乐";
                // 同目录同名 .lrc 歌词文件（大小写都认）
                String base = f.getName().substring(0, f.getName().lastIndexOf('.'));
                File lrc = new File(f.getParentFile(), base + ".lrc");
                if (!lrc.exists()) lrc = new File(f.getParentFile(), base + ".LRC");
                if (lrc.exists() && lrc.isFile()) {
                    t.lyricFile = Uri.fromFile(lrc).toString();
                }
                out.add(t);
            }
        }
    }

    private boolean isAudioFile(String name) {
        if (name == null) return false;
        String n = name.toLowerCase();
        for (String e : AUDIO_EXTS) {
            if (n.endsWith("." + e)) return true;
        }
        return false;
    }

    private String stripExt(String name) {
        int i = name.lastIndexOf('.');
        return i > 0 ? name.substring(0, i) : name;
    }

    /** 本地文件优先读音频自带标题，避免乱码文件名；读不到再回退文件名 */
    private String readLocalTitle(File f) {
        String fallback = stripExt(f.getName());
        try {
            android.media.MediaMetadataRetriever mmr = new android.media.MediaMetadataRetriever();
            mmr.setDataSource(f.getAbsolutePath());
            String meta = mmr.extractMetadata(
                    android.media.MediaMetadataRetriever.METADATA_KEY_TITLE);
            mmr.release();
            if (meta != null && !meta.trim().isEmpty()) {
                return meta.trim();
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    // ================= 我的歌单 =================

    private void showCreatePlaylist() {
        EditText et = new EditText(getContext());
        et.setHint("歌单名称");
        et.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                com.redmusic.player.util.ThemeManager.primaryColor(getContext())));
        android.graphics.drawable.ShapeDrawable caret = new android.graphics.drawable.ShapeDrawable();
        caret.setIntrinsicWidth(dp(2));
        caret.getPaint().setColor(com.redmusic.player.util.ThemeManager.primaryColor(getContext()));
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            et.setTextCursorDrawable(caret);
        }
        final AlertDialog d = new AlertDialog.Builder(getContext())
                .setTitle("新建歌单")
                .setView(et)
                .setPositiveButton("创建", (dlg, w) -> {
                    String name = et.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(getContext(), "名称不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    List<Playlist> pls = Prefs.getPlaylists();
                    pls.add(new Playlist(String.valueOf(System.currentTimeMillis()), name));
                    Prefs.savePlaylists(pls);
                    refresh();
                })
                .setNegativeButton("取消", null)
                .create();
        d.show();
        int pc = com.redmusic.player.util.ThemeManager.primaryColor(getContext());
        d.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(pc);
        d.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(pc);
    }

    private void openPlaylistDetail(Playlist p) {
        inPlaylistDetail = true;
        openPlaylistId = p.id;
        refresh();
    }

    private void backToPlaylists() {
        inPlaylistDetail = false;
        openPlaylistId = null;
        refresh();
    }

    private Playlist findPlaylist(String id) {
        if (id == null) return null;
        for (Playlist p : Prefs.getPlaylists()) {
            if (id.equals(p.id)) return p;
        }
        return null;
    }

    /** 歌单详情里点"添加歌曲"：可选本地导入 / 在线歌曲库 */
    private void showAddSongsToOpen() {
        final AlertDialog dlg = new AlertDialog.Builder(getContext())
                .setTitle("添加歌曲到歌单")
                .setItems(new String[]{"从本地导入添加", "从在线歌曲库添加"}, (d, w) -> {
                    if (w == 0) {
                        showAddImportedSongsToOpen();
                    } else {
                        showAddOnlineSongsToOpen();
                    }
                })
                .setNegativeButton("取消", null)
                .create();
        dlg.show();
        int pc = com.redmusic.player.util.ThemeManager.primaryColor(getContext());
        dlg.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(pc);
    }

    /** 从本地导入列表多选添加 */
    private void showAddImportedSongsToOpen() {
        List<Track> imported = Prefs.getImportedTracks();
        if (imported.isEmpty()) {
            Toast.makeText(getContext(), "请先在\"导入\"里扫描添加本地歌曲", Toast.LENGTH_LONG).show();
            return;
        }
        final boolean[] checked = new boolean[imported.size()];
        String[] names = new String[imported.size()];
        for (int i = 0; i < imported.size(); i++) {
            names[i] = imported.get(i).displayTitle() + " - " + imported.get(i).displayArtist();
        }
        final AlertDialog dlg = new AlertDialog.Builder(getContext())
                .setTitle("选择要加入歌单的歌曲")
                .setMultiChoiceItems(names, checked, (d, w, is) -> checked[w] = is)
                .setPositiveButton("添加", (d, w) -> {
                    Playlist p = findPlaylist(openPlaylistId);
                    if (p == null) return;
                    int added = 0;
                    for (int i = 0; i < imported.size(); i++) {
                        if (checked[i] && PlaylistHelper.addNoSave(p, imported.get(i))) added++;
                    }
                    PlaylistHelper.save(p);
                    Toast.makeText(getContext(), "已添加 " + added + " 首", Toast.LENGTH_SHORT).show();
                    refresh();
                })
                .setNegativeButton("取消", null)
                .create();
        dlg.show();
        int pc = com.redmusic.player.util.ThemeManager.primaryColor(getContext());
        dlg.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(pc);
        dlg.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(pc);
    }

    /** 从在线歌曲库搜索添加（输入即自动搜索） */
    private void showAddOnlineSongsToOpen() {
        if (getContext() == null) return;
        LinearLayout content = new LinearLayout(getContext());
        content.setOrientation(LinearLayout.VERTICAL);
        LinearLayout searchRow = new LinearLayout(getContext());
        searchRow.setOrientation(LinearLayout.HORIZONTAL);
        EditText input = new EditText(getContext());
        input.setHint("输入歌名 / 歌手，自动搜索");
        input.setSingleLine(true);
        input.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        input.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                com.redmusic.player.util.ThemeManager.primaryColor(getContext())));
        android.graphics.drawable.ShapeDrawable caret =
                new android.graphics.drawable.ShapeDrawable();
        caret.setIntrinsicWidth(dp(2));
        caret.getPaint().setColor(com.redmusic.player.util.ThemeManager.primaryColor(getContext()));
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            input.setTextCursorDrawable(caret);
        }
        Button go = new Button(getContext());
        go.setText("搜索");
        searchRow.addView(input, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        searchRow.addView(go);
        TextView hint = new TextView(getContext());
        hint.setText("勾选歌曲后，点下方\"加入选中\"");
        hint.setTextSize(12);
        hint.setTextColor(0xFF999999);
        hint.setPadding(dp(18), dp(6), dp(18), dp(4));
        ListView lv = new ListView(getContext());
        lv.setDividerHeight(0);
        content.addView(searchRow);
        content.addView(hint);
        content.addView(lv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(320)));

        final List<Track> results = new ArrayList<>();
        final List<String> names = new ArrayList<>();
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_list_item_multiple_choice, names);
        lv.setAdapter(adapter);
        lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        final AlertDialog dlg = new AlertDialog.Builder(getContext())
                .setTitle("添加在线歌曲")
                .setView(content)
                .setPositiveButton("加入选中", (d, w) -> {
                    Playlist p = findPlaylist(openPlaylistId);
                    if (p == null) return;
                    int added = 0;
                    for (int i = 0; i < results.size(); i++) {
                        if (lv.isItemChecked(i)) {
                            if (PlaylistHelper.addNoSave(p, results.get(i))) added++;
                        }
                    }
                    PlaylistHelper.save(p);
                    Toast.makeText(getContext(), "已添加 " + added + " 首", Toast.LENGTH_SHORT).show();
                    refresh();
                })
                .setNegativeButton("取消", null)
                .create();

        // 输入防抖后自动搜索
        final Runnable debounced = new Runnable() {
            @Override
            public void run() {
                doOnlineSearch(input, results, names, adapter);
            }
        };
        input.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                main.removeCallbacks(debounced);
                if (s.toString().trim().isEmpty()) return;
                main.postDelayed(debounced, 300);
            }
        });
        go.setOnClickListener(v -> {
            main.removeCallbacks(debounced);
            doOnlineSearch(input, results, names, adapter);
        });
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                main.removeCallbacks(debounced);
                doOnlineSearch(input, results, names, adapter);
                return true;
            }
            return false;
        });
        input.requestFocus();
        dlg.show();
        int pc = com.redmusic.player.util.ThemeManager.primaryColor(getContext());
        dlg.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(pc);
        dlg.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(pc);
    }

    /** 在线搜索并填充结果列表 */
    private void doOnlineSearch(EditText input, List<Track> results, List<String> names,
                                ArrayAdapter<String> adapter) {
        if (getContext() == null) return;
        String q = input.getText().toString().trim();
        if (q.isEmpty()) {
            Toast.makeText(getContext(), "请输入关键词", Toast.LENGTH_SHORT).show();
            return;
        }
        MusicApi.search(q, new MusicApi.TrackListCallback() {
            @Override
            public void onSuccess(List<Track> tracks) {
                main.post(() -> {
                    results.clear();
                    names.clear();
                    results.addAll(tracks);
                    for (Track t : tracks) names.add(t.displayTitle() + " - " + t.displayArtist());
                    adapter.notifyDataSetChanged();
                    if (tracks.isEmpty()) {
                        Toast.makeText(getContext(), "没有找到相关歌曲", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String msg) {
                main.post(() ->
                        Toast.makeText(getContext(), "网络错误：" + msg, Toast.LENGTH_SHORT).show());
            }
        });
    }

    /** 歌单详情长按歌曲 → 从歌单移除 */
    private void showRemoveFromOpenPlaylist(Track t) {
        final AlertDialog dlg = new AlertDialog.Builder(getContext())
                .setTitle("从歌单移除")
                .setMessage("确定把《" + t.displayTitle() + "》从歌单移除？")
                .setPositiveButton("移除", (d, w) -> {
                    Playlist p = findPlaylist(openPlaylistId);
                    if (p == null) return;
                    List<Track> keep = new ArrayList<>();
                    for (Track x : p.tracks) {
                        if (!(t.id != null && t.id.equals(x.id))) keep.add(x);
                    }
                    p.tracks = keep;
                    PlaylistHelper.save(p);
                    refresh();
                })
                .setNegativeButton("取消", null)
                .create();
        dlg.show();
        int pc = com.redmusic.player.util.ThemeManager.primaryColor(getContext());
        dlg.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(pc);
        dlg.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(pc);
    }

    private void deleteOpenPlaylist() {
        Playlist p = findPlaylist(openPlaylistId);
        if (p == null) return;
        final AlertDialog dlg = new AlertDialog.Builder(getContext())
                .setTitle("删除歌单")
                .setMessage("确定删除歌单《" + p.name + "》？")
                .setPositiveButton("删除", (d, w) -> {
                    List<Playlist> pls = Prefs.getPlaylists();
                    List<Playlist> keep = new ArrayList<>();
                    for (Playlist x : pls) {
                        if (!x.id.equals(openPlaylistId)) keep.add(x);
                    }
                    Prefs.savePlaylists(keep);
                    backToPlaylists();
                })
                .setNegativeButton("取消", null)
                .create();
        dlg.show();
        int pc = com.redmusic.player.util.ThemeManager.primaryColor(getContext());
        dlg.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(pc);
        dlg.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(pc);
    }
}
