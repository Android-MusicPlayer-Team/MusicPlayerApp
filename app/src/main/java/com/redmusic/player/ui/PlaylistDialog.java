package com.redmusic.player.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
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
import com.redmusic.player.util.ThemeManager;

import java.util.List;

/** 底部弹出的播放列表面板：显示当前队列，可切歌、可删除（删除后不关闭，按顶部才返回；随白天/夜间） */
public class PlaylistDialog extends Dialog {

    private final LinearLayout root;

    public PlaylistDialog(Context ctx) {
        super(ctx, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        Window w = getWindow();
        if (w != null) {
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            boolean dark = ThemeManager.isDark(ctx);
            w.setBackgroundDrawable(new ColorDrawable(
                    dark ? 0xEE000000 : 0xEEF5F5F5));
        }
        root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(ctx, 24), dp(ctx, 28), dp(ctx, 24), dp(ctx, 24));
        setContentView(root);
        refresh(ctx);
    }

    /** 重建弹窗内容（删除歌曲后调用，不关闭弹窗） */
    private void refresh(Context ctx) {
        root.removeAllViews();
        PlayerHolder ph = PlayerHolder.get();
        List<Track> queue = ph.getQueue();
        int curPos = ph.getQueuePosition();
        boolean dark = ThemeManager.isDark(ctx);
        int text = ThemeManager.primaryTextColor(ctx);
        int sub = ThemeManager.secondaryTextColor(ctx);
        int pc = ThemeManager.primaryColor(ctx);

        // 标题栏：左标题右退出（按这里才返回）
        LinearLayout titleBar = new LinearLayout(ctx);
        titleBar.setOrientation(LinearLayout.HORIZONTAL);
        titleBar.setGravity(Gravity.CENTER_VERTICAL);
        titleBar.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView title = new TextView(ctx);
        title.setText("播放列表 (" + queue.size() + ")");
        title.setTextColor(text);
        title.setTextSize(18);
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        title.setLayoutParams(tl);
        titleBar.addView(title);
        TextView closeX = new TextView(ctx);
        closeX.setText("✕");
        closeX.setTextColor(text);
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
            empty.setTextColor(sub);
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
            idx.setTextColor(i == cur ? pc : sub);
            idx.setTextSize(14);
            idx.setWidth(dp(ctx, 36));
            row.addView(idx);

            LinearLayout col = new LinearLayout(ctx);
            col.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            col.setLayoutParams(colLp);
            TextView name = new TextView(ctx);
            name.setText(t.displayTitle());
            name.setTextColor(i == cur ? pc : text);
            name.setTextSize(15);
            name.setSingleLine(true);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            col.addView(name);
            TextView artist = new TextView(ctx);
            artist.setText(t.displayArtist());
            artist.setTextColor(sub);
            artist.setTextSize(12);
            artist.setSingleLine(true);
            artist.setEllipsize(android.text.TextUtils.TruncateAt.END);
            col.addView(artist);
            row.addView(col);

            // 删除：只删这一首，弹窗不关闭，按顶部 ✕ 才返回
            TextView del = new TextView(ctx);
            del.setText("✕");
            del.setTextColor(sub);
            del.setTextSize(16);
            del.setPadding(dp(ctx, 16), 0, dp(ctx, 8), 0);
            final int pos = i;
            del.setOnClickListener(v -> {
                ph.removeAt(pos);
                Toast.makeText(ctx, "已移除", Toast.LENGTH_SHORT).show();
                refresh(ctx);
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
    }

    private int dp(Context c, int v) {
        return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f);
    }
}
