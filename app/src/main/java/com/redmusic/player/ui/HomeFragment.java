package com.redmusic.player.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.redmusic.player.R;
import com.redmusic.player.api.MusicApi;
import com.redmusic.player.model.Track;
import com.redmusic.player.player.PlayerHolder;
import com.redmusic.player.util.Prefs;

import java.util.List;

public class HomeFragment extends Fragment {

    private final Handler main = new Handler(Looper.getMainLooper());
    private TrackAdapter adapter;
    private SwipeRefreshLayout refresh;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        adapter = new TrackAdapter(true, false);
        adapter.setAction(new TrackAdapter.OnAction() {
            @Override
            public void onPlay(int position) {
                playList(position);
            }

            @Override
            public void onFav(Track t) {
                Prefs.toggleFav(t);
                Toast.makeText(requireContext(),
                        Prefs.isFav(t) ? "已收藏 ♡" : "已取消收藏",
                        Toast.LENGTH_SHORT).show();
                int p = adapter.getTracks().indexOf(t);
                if (p >= 0) adapter.notifyItemChanged(p);
            }
        });
        RecyclerView list = view.findViewById(R.id.home_list);
        list.setLayoutManager(new LinearLayoutManager(getContext()));
        list.setAdapter(adapter);

        refresh = view.findViewById(R.id.home_refresh);
        refresh.setOnRefreshListener(this::loadTop);
        loadTop();

        // 头部信息
        TextView header = view.findViewById(R.id.home_header);
        header.setText("全球热歌榜 Top 50 · 来自 iTunes 公开曲库");
    }

    private void loadTop() {
        MusicApi.topSongs(new MusicApi.TrackListCallback() {
            @Override
            public void onSuccess(List<Track> tracks) {
                main.post(() -> {
                    adapter.setTracks(tracks);
                    refresh.setRefreshing(false);
                    if (tracks.isEmpty()) {
                        Toast.makeText(getContext(), "榜单加载失败，请检查网络", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String msg) {
                main.post(() -> {
                    refresh.setRefreshing(false);
                    Toast.makeText(getContext(), "网络错误：" + msg, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void playList(int position) {
        List<Track> list = adapter.getTracks();
        if (list.isEmpty()) return;
        Track t = list.get(position);
        Toast.makeText(getContext(), "加载播放源...", Toast.LENGTH_SHORT).show();
        // 排行榜歌曲无预览地址，先按歌名解析
        MusicApi.resolvePreview(t, new MusicApi.TrackListCallback() {
            @Override
            public void onSuccess(List<Track> one) {
                main.post(() -> {
                    list.set(position, one.get(0));
                    PlayerHolder.get().playQueue(list, position);
                    Prefs.addRecent(one.get(0));
                    if (getContext() != null) {
                        startActivity(new android.content.Intent(getContext(), PlayerActivity.class));
                    }
                });
            }

            @Override
            public void onError(String msg) {
                main.post(() -> Toast.makeText(getContext(), "获取播放源失败", Toast.LENGTH_SHORT).show());
            }
        });
    }
}
