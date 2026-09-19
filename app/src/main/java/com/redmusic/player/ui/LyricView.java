package com.redmusic.player.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.redmusic.player.model.LyricLine;

import java.util.List;

/** 滚动歌词视图：当前行高亮放大，可切换全屏模式 */
public class LyricView extends View {
    private List<LyricLine> lines;
    private long positionMs = 0;
    private int currentIndex = -1;
    private final Paint activePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint normalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint activeBack = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float lineHeight;
    private boolean fullScreen = false;

    public LyricView(Context context) {
        this(context, null);
    }

    public LyricView(Context context, AttributeSet attrs) {
        super(context, attrs);
        activePaint.setColor(0xFFC20C0C);
        activePaint.setTextSize(dp(18));
        activePaint.setFakeBoldText(true);
        activePaint.setTextAlign(Paint.Align.CENTER);
        normalPaint.setColor(0xB3FFFFFF);
        normalPaint.setTextSize(dp(15));
        normalPaint.setTextAlign(Paint.Align.CENTER);
        activeBack.setColor(0x66C20C0C);
        lineHeight = dp(30);
    }

    public void setFullScreen(boolean fs) {
        fullScreen = fs;
        if (fs) {
            activePaint.setTextSize(dp(22));
            normalPaint.setTextSize(dp(17));
            lineHeight = dp(38);
        } else {
            activePaint.setTextSize(dp(18));
            normalPaint.setTextSize(dp(15));
            lineHeight = dp(30);
        }
        invalidate();
    }

    public boolean isFullScreen() {
        return fullScreen;
    }

    public void setLines(List<LyricLine> lines) {
        this.lines = lines;
        currentIndex = -1;
        positionMs = 0;
        invalidate();
    }

    public void updatePosition(long pos) {
        positionMs = pos;
        if (lines == null || lines.isEmpty()) {
            invalidate();
            return;
        }
        int idx = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (positionMs >= lines.get(i).timeMs) {
                idx = i;
            } else {
                break;
            }
        }
        if (idx != currentIndex) {
            currentIndex = idx;
            invalidate();
        }
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (lines == null || lines.isEmpty()) {
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(0x66FFFFFF);
            p.setTextSize(dp(14));
            p.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(fullScreen ? "暂无歌词，滑动播放封面" : "歌词加载中...", getWidth() / 2f, getHeight() / 2f, p);
            return;
        }
        int center = getHeight() / 2;
        if (currentIndex < 0) currentIndex = 0;
        if (currentIndex >= lines.size()) currentIndex = lines.size() - 1;

        // 当前行高亮背景
        float activeCenterY = center;
        canvas.drawRoundRect(getWidth() / 2f - dp(120), activeCenterY - lineHeight / 2f + dp(2),
                getWidth() / 2f + dp(120), activeCenterY + lineHeight / 2f - dp(2), dp(10), dp(10), activeBack);

        // 当前行
        Paint.FontMetrics fm = activePaint.getFontMetrics();
        canvas.drawText(lines.get(currentIndex).text, getWidth() / 2f,
                activeCenterY - (fm.ascent + fm.descent) / 2f, activePaint);

        // 前 3 行 / 后 4 行
        for (int offset = 1; offset <= 4; offset++) {
            int idx = currentIndex + offset;
            if (idx < lines.size()) {
                float y = activeCenterY + offset * lineHeight;
                canvas.drawText(lines.get(idx).text, getWidth() / 2f, y, normalPaint);
            }
            idx = currentIndex - offset;
            if (idx >= 0) {
                float y = activeCenterY - offset * lineHeight;
                canvas.drawText(lines.get(idx).text, getWidth() / 2f, y, normalPaint);
            }
        }
    }
}
