package com.redmusic.player.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.redmusic.player.model.Track;
import com.redmusic.player.player.PlayerHolder;

import java.util.List;

/** 底部弹出的播放列表面板：显示当前队列，可切歌、可删除 */
public class PlaylistDialog extends Dialog {

    public PlaylistDialog(Context ctx) {
        super(ctx, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        Window w = getWindow();
        if (w != null) {
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0xEE000000));
        }
        build(ctx);
    }

    private void build(Context ctx) {
        PlayerHolder ph = PlayerHolder.get();
        List<Track> queue = ph.getQueue();
        int curPos = ph.getQueuePosition();

        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(ctx, 24), dp(ctx, 28), dp(ctx, 24), dp(ctx, 24));

        // 标题栏：左标题右退出
        LinearLayout titleBar = new LinearLayout(ctx);
        titleBar.setOrientation(LinearLayout.HORIZONTAL);
        titleBar.setGravity(Gravity.CENTER_VERTICAL);
        titleBar.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView title = new TextView(ctx);
        title.setText("播放列表 (" + queue.size() + ")");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        title.setLayoutParams(tl);
        titleBar.addView(title);
        TextView closeX = new TextView(ctx);
        closeX.setText("✕");
        closeX.setTextColor(0xFFFFFFFF);
        closeX.setTextSize(22);
        closeX.setPadding(dp(ctx, 20), 0, dp(ctx, 4), 0);
        closeX.setOnClickListener(v -> dismiss());
        titleBar.addView(closeX);
        root.addView(titleBar);

        // 可滚动列表
        ScrollView sv = new ScrollView(ctx);
        LinearLayout list = new LinearLayout(ctx);
        list.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams listLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        list.setLayoutParams(listLp);

        if (queue.isEmpty()) {
            TextView empty = new TextView(ctx);
            empty.setText("队列为空");
            empty.setTextColor(0x88FFFFFF);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(ctx, 60), 0, 0);
            list.addView(empty);
        }

        int cur = curPos;
        for (int i = 0; i < queue.size(); i++) {
            Track t = queue.get(i);
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(ctx, 14), 0, dp(ctx, 14));

            TextView idx = new TextView(ctx);
            idx.setText(i == cur ? "♪" : String.valueOf(i + 1));
            idx.setTextColor(i == cur ? 0xFFFFFFFF : 0x66FFFFFF);
            idx.setTextSize(14);
            idx.setWidth(dp(ctx, 36));
            row.addView(idx);

            LinearLayout col = new LinearLayout(ctx);
            col.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            col.setLayoutParams(colLp);
            TextView name = new TextView(ctx);
            name.setText(t.displayTitle());
            name.setTextColor(i == cur ? 0xFFFFFFFF : 0xDDFFFFFF);
            name.setTextSize(15);
            name.setSingleLine(true);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            col.addView(name);
            TextView artist = new TextView(ctx);
            artist.setText(t.displayArtist());
            artist.setTextColor(0x77FFFFFF);
            artist.setTextSize(12);
            artist.setSingleLine(true);
            artist.setEllipsize(android.text.TextUtils.TruncateAt.END);
            col.addView(artist);
            row.addView(col);

            TextView del = new TextView(ctx);
            del.setText("✕");
            del.setTextColor(0x88FFFFFF);
            del.setTextSize(16);
            del.setPadding(dp(ctx, 16), 0, dp(ctx, 8), 0);
            final int pos = i;
            del.setOnClickListener(v -> {
                ph.removeAt(pos);
                Toast.makeText(ctx, "已移除", Toast.LENGTH_SHORT).show();
                dismiss();
            });
            row.addView(del);

            final int posF = i;
            row.setOnClickListener(v -> {
                ph.playAt(posF);
                dismiss();
            });

            list.addView(row);
        }

        sv.addView(list);
        root.addView(sv);

        setContentView(root);
    }

    private int dp(Context c, int v) {
        return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f);
    }
}
