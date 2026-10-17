package com.redmusic.player.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.redmusic.player.model.LyricLine;
import com.redmusic.player.util.ThemeManager;

import java.util.List;
import java.util.Locale;

/** 滚动歌词视图：当前行高亮放大，随播放进度平滑滚动，上下行远近淡入淡出；支持拖动歌词定位播放位置 */
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

    /** 拖动歌词定位到播放位置的回调 */
    public interface OnSeekListener {
        void onSeekTo(long timeMs);
    }

    private OnSeekListener onSeek;
    /** 是否正在拖动歌词 */
    private boolean dragging = false;
    /** 拖动中的预览时间（目标播放位置） */
    private long previewPositionMs = 0;
    /** 按下时记录的基准 */
    private long dragStartPosMs = 0;
    private float dragStartY = 0f;

    public LyricView(Context context) {
        this(context, null);
    }

    public LyricView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // 歌词颜色随日夜：夜间白色，白天深色（配合播放页背景）；当前行高亮用主题主色
        boolean dark = com.redmusic.player.util.ThemeManager.isDark(getContext());
        int fg = dark ? 0xFFFFFFFF : 0xFF33363D;
        int pc = com.redmusic.player.util.ThemeManager.primaryColor(getContext());
        activePaint.setColor(pc);
        activePaint.setFakeBoldText(true);
        activePaint.setTextAlign(Paint.Align.CENTER);
        normalPaint.setColor(fg);
        normalPaint.setTextAlign(Paint.Align.CENTER);
        activeBack.setColor(dark ? 0x22FFFFFF : 0x22000000);
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

    public void setOnSeekListener(OnSeekListener l) {
        onSeek = l;
    }

    /** 根据时间找所在歌词行 */
    private int findIndex(long pos) {
        if (lines == null || lines.isEmpty()) return 0;
        int cur = 0;
        for (int i = 0; i < lines.size(); i++) {
            if (pos >= lines.get(i).timeMs) cur = i;
            else break;
        }
        return cur;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (lines == null || lines.isEmpty()) return super.onTouchEvent(ev);
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                dragging = true;
                dragStartY = ev.getY();
                dragStartPosMs = positionMs;
                previewPositionMs = positionMs;
                getParent().requestDisallowInterceptTouchEvent(true);
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE: {
                // 上滑（y 减小）→ 更晚时间，下滑 → 更早时间；按行换算
                float lineDelta = (dragStartY - ev.getY()) / lineHeight;
                int startLine = findIndex(dragStartPosMs);
                int target = startLine + Math.round(lineDelta);
                if (target < 0) target = 0;
                if (target >= lines.size()) target = lines.size() - 1;
                previewPositionMs = lines.get(target).timeMs;
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                dragging = false;
                if (onSeek != null) onSeek.onSeekTo(previewPositionMs);
                invalidate();
                return true;
            }
        }
        return super.onTouchEvent(ev);
    }

    private String fmtTime(long ms) {
        long s = ms / 1000;
        return String.format(Locale.US, "%02d:%02d", s / 60, s % 60);
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (lines == null || lines.isEmpty()) {
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            boolean dark = com.redmusic.player.util.ThemeManager.isDark(getContext());
            p.setColor(dark ? 0x66FFFFFF : 0x66FF3336);
            p.setTextSize(dp(14));
            p.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("暂无歌词", getWidth() / 2f, getHeight() / 2f, p);
            return;
        }
        int center = getHeight() / 2;

        // 拖动歌词时用预览时间定位，否则跟随播放进度
        long drawPos = dragging ? previewPositionMs : positionMs;

        // 找当前行
        int cur = findIndex(drawPos);
        // 在当前行与下一行之间插值，实现平滑滚动
        float frac = 0f;
        if (cur < lines.size() - 1) {
            long curT = lines.get(cur).timeMs;
            long nextT = lines.get(cur + 1).timeMs;
            if (nextT > curT) {
                frac = (drawPos - curT) / (float) (nextT - curT);
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
                // 当前行：文字中间为主题色、向两边减淡到普通歌词色（无胶囊背景）
                activePaint.setAlpha(255);
                float w = activePaint.measureText(text);
                float cx = getWidth() / 2f;
                float fade = w * 0.7f; // 减淡范围：向歌词两端各扩展 70%，让淡化发生在歌词之外
                float left = cx - w / 2f - fade;
                float right = cx + w / 2f + fade;
                int fg = ThemeManager.isDark(getContext()) ? 0xFFFFFFFF : 0xFF33363D;
                int pc = ThemeManager.primaryColor(getContext());
                activePaint.setShader(new LinearGradient(
                        left, 0f, right, 0f,
                        new int[]{fg, pc, pc, pc, fg},
                        new float[]{0f, 0.28f, 0.5f, 0.72f, 1f},
                        Shader.TileMode.CLAMP));
                Paint.FontMetrics fm = activePaint.getFontMetrics();
                canvas.drawText(text, getWidth() / 2f, y - (fm.ascent + fm.descent) / 2f, activePaint);
                activePaint.setShader(null);
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

        // 拖动歌词时：顶部显示目标时间提示
        if (dragging) {
            Paint tip = new Paint(Paint.ANTI_ALIAS_FLAG);
            boolean dark = com.redmusic.player.util.ThemeManager.isDark(getContext());
            tip.setColor(dark ? 0xFFFFFFFF : 0xFF33363D);
            tip.setTextSize(dp(13));
            tip.setTextAlign(Paint.Align.CENTER);
            String t = fmtTime(previewPositionMs);
            float tw = tip.measureText(t);
            float pad = dp(14);
            float cx = getWidth() / 2f;
            float cy = center - lineHeight * 2.4f;
            canvas.drawRoundRect(cx - tw / 2f - pad, cy - dp(16),
                    cx + tw / 2f + pad, cy + dp(16), dp(16), dp(16), activeBack);
            canvas.drawText(t, cx, cy - (tip.getFontMetrics().ascent + tip.getFontMetrics().descent) / 2f, tip);
        }
    }
}
