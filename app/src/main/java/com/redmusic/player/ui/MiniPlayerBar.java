package com.redmusic.player.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.redmusic.player.R;
import com.redmusic.player.model.Track;
import com.redmusic.player.player.PlayerHolder;

/** 底部迷你播放栏 */
public class MiniPlayerBar extends LinearLayout {
    private ImageView cover;
    private TextView title, artist;
    private ImageButton playBtn, playlistBtn;

    public MiniPlayerBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.mini_player_bar, this, true);
        setOrientation(HORIZONTAL);
        cover = findViewById(R.id.mini_cover);
        title = findViewById(R.id.mini_title);
        artist = findViewById(R.id.mini_artist);
        playBtn = findViewById(R.id.mini_play);
        playBtn.setOnClickListener(v -> PlayerHolder.get().playPause());
        playlistBtn = findViewById(R.id.mini_playlist);
        playlistBtn.setOnClickListener(v -> new PlaylistDialog(getContext()).show());
    }

    public void refresh() {
        PlayerHolder ph = PlayerHolder.get();
        Track t = ph.currentTrack();
        if (t == null) {
            title.setText("未在播放");
            artist.setText("去发现好音乐");
            cover.setImageResource(R.drawable.ic_music_note);
            playBtn.setImageResource(R.drawable.ic_play);
            return;
        }
        title.setText(t.displayTitle());
        artist.setText(t.displayArtist());
        if (t.artworkUrl != null && !t.artworkUrl.isEmpty()) {
            Glide.with(getContext())
                    .load(t.artworkUrl)
                    .apply(new RequestOptions().transform(new CircleCrop()))
                    .into(cover);
        } else {
            cover.setImageResource(R.drawable.ic_music_note);
        }
        playBtn.setImageResource(ph.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
    }
}
