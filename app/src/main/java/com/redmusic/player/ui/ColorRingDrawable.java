package com.redmusic.player.ui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.drawable.Drawable;

/** 彩色色环 Drawable：表示"自定义主题色"入口（彩虹环，中间挖空） */
public class ColorRingDrawable extends Drawable {

    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint holePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int[] hueColors;
    private boolean selected;

    public ColorRingDrawable() {
        hueColors = new int[361];
        for (int i = 0; i <= 360; i++) hueColors[i] = Color.HSVToColor(new float[]{i, 1f, 1f});
        holePaint.setColor(0x00000000);
    }

    public void setSelected(boolean sel) {
        this.selected = sel;
        invalidateSelf();
    }

    @Override
    public void draw(Canvas canvas) {
        int w = getBounds().width();
        int h = getBounds().height();
        if (w <= 0 || h <= 0) return;
        int cx = w / 2, cy = h / 2;
        int r = Math.min(w, h) / 2 - 1;
        int innerR = (int) (r * 0.62f);

        // 彩虹环
        ringPaint.setShader(new SweepGradient(cx, cy, hueColors, null));
        ringPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx, cy, r, ringPaint);
        ringPaint.setShader(null);

        // 中间挖空（dst 清除）
        holePaint.setColor(0x00000000);
        holePaint.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR));
        canvas.drawCircle(cx, cy, innerR, holePaint);
        holePaint.setXfermode(null);

        // 选中白描边
        if (selected) {
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(3f);
            strokePaint.setColor(0xFFFFFFFF);
            canvas.drawCircle(cx, cy, r, strokePaint);
        }
    }

    @Override
    public void setAlpha(int alpha) {}

    @Override
    public void setColorFilter(android.graphics.ColorFilter cf) {}

    @Override
    public int getOpacity() {
        return android.graphics.PixelFormat.TRANSLUCENT;
    }
}
