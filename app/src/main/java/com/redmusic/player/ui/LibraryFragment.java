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

import com.redmusic.player.R;
import com.redmusic.player.model.Track;
import com.redmusic.player.player.PlayerHolder;
import com.redmusic.player.util.Prefs;

import java.util.List;

public class LibraryFragment extends Fragment {

    private final Handler main = new Handler(Looper.getMainLooper());
    private TrackAdapter adapter;
    private TextView tabFav, tabRecent, emptyTip;
    private boolean favMode = true;

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
        emptyTip = view.findViewById(R.id.lib_empty);

        adapter = new TrackAdapter(false, true);
        adapter.setAction(new TrackAdapter.OnAction() {
            @Override
            public void onPlay(int position) {
                List<Track> list = adapter.getTracks();
                if (list.isEmpty()) return;
                PlayerHolder.get().playQueue(list, position);
                Prefs.addRecent(list.get(position));
                startActivity(new android.content.Intent(getContext(), PlayerActivity.class));
            }

            @Override
            public void onFav(Track t) {
                Prefs.toggleFav(t);
                refresh();
                Toast.makeText(getContext(), Prefs.isFav(t) ? "已收藏" : "已取消收藏", Toast.LENGTH_SHORT).show();
            }
        });
        RecyclerView list = view.findViewById(R.id.lib_list);
        list.setLayoutManager(new LinearLayoutManager(getContext()));
        list.setAdapter(adapter);

        tabFav.setOnClickListener(v -> {
            favMode = true;
            updateTabs();
            refresh();
        });
        tabRecent.setOnClickListener(v -> {
            favMode = false;
            updateTabs();
            refresh();
        });
        updateTabs();
        refresh();
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    private void updateTabs() {
        tabFav.setTextColor(favMode ? 0xFFC20C0C : 0xFFAAAAAA);
        tabRecent.setTextColor(favMode ? 0xFFAAAAAA : 0xFFC20C0C);
    }

    private void refresh() {
        List<Track> list = favMode ? Prefs.getFavs() : Prefs.getRecents();
        adapter.setTracks(list);
        if (list.isEmpty()) {
            emptyTip.setVisibility(View.VISIBLE);
            emptyTip.setText(favMode ? "还没有收藏的歌曲\n在歌曲列表点 ♥ 收藏" : "还没有播放记录");
        } else {
            emptyTip.setVisibility(View.GONE);
        }
    }
}
