package com.redmusic.player.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.redmusic.player.R;
import com.redmusic.player.model.Track;
import com.redmusic.player.util.Prefs;
import com.redmusic.player.util.ThemeManager;

import java.util.ArrayList;
import java.util.List;

/** 通用歌曲列表适配器：支持排行榜模式（显示序号）与收藏模式（显示红心） */
public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.VH> {

    public interface OnAction {
        void onPlay(int position);

        void onFav(Track t);

        /** 长按歌曲（默认空实现，不强制各界面实现） */
        default void onLongPress(Track t) {
        }

        /** 点行内"删除"按钮（默认空实现） */
        default void onRemove(Track t) {
        }
    }

    private final List<Track> tracks = new ArrayList<>();
    private final boolean rankMode;
    private final boolean favMode;
    private boolean removeMode = false;
    private OnAction action;

    public TrackAdapter(boolean rankMode, boolean favMode) {
        this.rankMode = rankMode;
        this.favMode = favMode;
    }

    public void setAction(OnAction action) {
        this.action = action;
    }

    /** 是否显示行内"删除"按钮（歌单详情用） */
    public void setRemoveMode(boolean b) {
        removeMode = b;
    }

    public void setTracks(List<Track> list) {
        tracks.clear();
        if (list != null) tracks.addAll(list);
        notifyDataSetChanged();
    }

    public List<Track> getTracks() {
        return tracks;
    }

    public Track get(int pos) {
        return tracks.get(pos);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_track, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Track t = tracks.get(position);
        h.title.setText(t.displayTitle());
        h.artist.setText(t.displayArtist());
        if (t.genre != null && !t.genre.isEmpty()) {
            h.artist.setText(t.displayArtist() + " · " + t.genre);
        }
        Context ctx = h.itemView.getContext();
        if (t.artworkUrl != null && !t.artworkUrl.isEmpty()) {
            Glide.with(ctx)
                    .load(t.artworkUrl)
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.ic_music_note)
                            .transform(new RoundedCorners(dp(ctx, 10))))
                    .into(h.cover);
        } else {
            h.cover.setImageResource(R.drawable.ic_music_note);
        }
        if (rankMode) {
            h.rank.setVisibility(View.VISIBLE);
            h.rank.setText(String.valueOf(t.rank > 0 ? t.rank : position + 1));
            h.rank.setTextColor(position < 3 ? ThemeManager.primaryColor(h.rank.getContext()) : 0xFF888888);
        } else {
            h.rank.setVisibility(View.GONE);
        }
        // 红心始终按收藏状态显示：收藏页全亮；最近播放/其他页未收藏则显示空心灰心
        h.fav.setVisibility(View.VISIBLE);
        boolean isFav = Prefs.isFav(t);
        h.fav.setImageResource(isFav ? R.drawable.ic_heart_filled : R.drawable.ic_heart);
        h.fav.setColorFilter(isFav ? ThemeManager.primaryColor(h.fav.getContext()) : 0xFF888888);
        h.itemView.setOnClickListener(v -> {
            if (action != null) action.onPlay(position);
        });
        h.itemView.setOnLongClickListener(v -> {
            if (action != null) action.onLongPress(t);
            return true;
        });
        h.fav.setOnClickListener(v -> {
            if (action != null) action.onFav(t);
        });
        if (removeMode) {
            h.remove.setVisibility(View.VISIBLE);
            h.remove.setTextColor(ThemeManager.primaryColor(h.remove.getContext()));
            h.remove.setTag(position);
            h.remove.setOnClickListener(v -> {
                if (action != null) action.onRemove(t);
            });
        } else {
            h.remove.setVisibility(View.GONE);
        }
    }

    private static int dp(Context ctx, float v) {
        return (int) (v * ctx.getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    public int getItemCount() {
        return tracks.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView cover;
        TextView title, artist, rank;
        ImageButton fav;
        TextView remove;

        VH(@NonNull View itemView) {
            super(itemView);
            cover = itemView.findViewById(R.id.item_cover);
            title = itemView.findViewById(R.id.item_title);
            artist = itemView.findViewById(R.id.item_artist);
            rank = itemView.findViewById(R.id.item_rank);
            fav = itemView.findViewById(R.id.item_fav);
            remove = itemView.findViewById(R.id.item_remove);
        }
    }
}
