package com.redmusic.player.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.redmusic.player.model.LyricLine;

import java.util.List;

/** 滚动歌词视图：当前行高亮放大，随播放进度平滑滚动，上下行远近淡入淡出 */
public class LyricView extends View {
    private List<LyricLine> lines;
    private long positionMs = 0;
    private final Paint activePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint normalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint activeBack = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float lineHeight;
    private boolean fullScreen = false;
    private float activeSize;
    private float normalSize;

    public LyricView(Context context) {
        this(context, null);
    }

    public LyricView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // 当前行纯白加粗，其余半透明白，提示条淡白胶囊
        activePaint.setColor(0xFFFFFFFF);
        activePaint.setFakeBoldText(true);
        activePaint.setTextAlign(Paint.Align.CENTER);
        normalPaint.setColor(0xFFFFFFFF);
        normalPaint.setTextAlign(Paint.Align.CENTER);
        activeBack.setColor(0x22FFFFFF);
        applySizes();
    }

    private void applySizes() {
        if (fullScreen) {
            activeSize = dp(17);
            normalSize = dp(15);
            lineHeight = dp(40);
        } else {
            activeSize = dp(15);
            normalSize = dp(13);
            lineHeight = dp(28);
        }
        activePaint.setTextSize(activeSize);
        normalPaint.setTextSize(normalSize);
    }

    public void setFullScreen(boolean fs) {
        fullScreen = fs;
        applySizes();
        invalidate();
    }

    public boolean isFullScreen() {
        return fullScreen;
    }

    public void setLines(List<LyricLine> lines) {
        this.lines = lines;
        positionMs = 0;
        invalidate();
    }

    public void updatePosition(long pos) {
        positionMs = pos;
        invalidate();
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
            canvas.drawText("暂无歌词", getWidth() / 2f, getHeight() / 2f, p);
            return;
        }
        int center = getHeight() / 2;

        // 找当前行
        int cur = 0;
        for (int i = 0; i < lines.size(); i++) {
            if (positionMs >= lines.get(i).timeMs) cur = i;
            else break;
        }
        // 在当前行与下一行之间插值，实现平滑滚动
        float frac = 0f;
        if (cur < lines.size() - 1) {
            long curT = lines.get(cur).timeMs;
            long nextT = lines.get(cur + 1).timeMs;
            if (nextT > curT) {
                frac = (positionMs - curT) / (float) (nextT - curT);
                if (frac < 0) frac = 0;
                if (frac > 1) frac = 1;
            }
        }

        for (int i = 0; i < lines.size(); i++) {
            float y = center + (i - cur - frac) * lineHeight;
            if (y < -lineHeight * 2 || y > getHeight() + lineHeight * 2) continue;

            // 距离中心的"行号"（当前行=0，往上往下递增）
            float dist = Math.abs(i - cur - frac);
            boolean isCur = (Math.round(i - cur - frac) == 0 && i == cur);

            String text = lines.get(i).text;

            if (isCur) {
                // 当前行：纯白、稍大、圆角胶囊底
                activePaint.setAlpha(255);
                float w = activePaint.measureText(text);
                float pad = dp(16);
                float left = getWidth() / 2f - w / 2f - pad;
                float right = getWidth() / 2f + w / 2f + pad;
                canvas.drawRoundRect(left, y - lineHeight / 2f + dp(4),
                        right, y + lineHeight / 2f - dp(4), dp(18), dp(18), activeBack);
                Paint.FontMetrics fm = activePaint.getFontMetrics();
                canvas.drawText(text, getWidth() / 2f, y - (fm.ascent + fm.descent) / 2f, activePaint);
            } else {
                // 上下行：距离越远越淡
                int alpha;
                if (dist <= 1f) {
                    // 紧挨着的两行：60% 不透明
                    alpha = 210;
                } else if (dist <= 2f) {
                    alpha = 150;
                } else if (dist <= 3f) {
                    alpha = 90;
                } else {
                    alpha = 50;
                }
                normalPaint.setAlpha(alpha);
                Paint.FontMetrics fm = normalPaint.getFontMetrics();
                canvas.drawText(text, getWidth() / 2f, y - (fm.ascent + fm.descent) / 2f, normalPaint);
            }
        }
    }
}
