package com.redmusic.player.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.redmusic.player.R;
import com.redmusic.player.model.Playlist;

import java.util.ArrayList;
import java.util.List;

/** 我的歌单列表适配器：显示歌单名 + 歌曲数，点击进入详情 */
public class PlaylistListAdapter extends RecyclerView.Adapter<PlaylistListAdapter.VH> {

    public interface OnAction {
        void onOpen(Playlist p);
    }

    private final List<Playlist> items = new ArrayList<>();
    private OnAction action;

    public void setAction(OnAction action) {
        this.action = action;
    }

    public void setItems(List<Playlist> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    public List<Playlist> getItems() {
        return items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_playlist, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Playlist p = items.get(position);
        h.name.setText(p.name);
        h.count.setText(p.tracks.size() + " 首");
        h.itemView.setOnClickListener(v -> {
            if (action != null) action.onOpen(p);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView name, count;

        VH(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.pl_name);
            count = itemView.findViewById(R.id.pl_count);
        }
    }
}
