package com.redmusic.player.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import java.util.Random;

/** 音频频谱柱状动画视图（Visualizer 数据驱动，无数据时自动呼吸动画） */
public class SpectrumView extends View {
    private final Paint barPaint = new Paint();
    private float[] levels;
    private final Random random = new Random();
    private boolean animating = false;
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!animating) return;
            generateRandomLevels();
            invalidate();
            postDelayed(this, 80);
        }
    };

    public SpectrumView(Context context) {
        this(context, null);
    }

    public SpectrumView(Context context, AttributeSet attrs) {
        super(context, attrs);
        barPaint.setShader(new LinearGradient(0, 0, 0, dp(36),
                0x80FFFFFF, 0x22FFFFFF, Shader.TileMode.CLAMP));
        levels = new float[48];
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    /** 由 Visualizer 提供真实波形数据（byte -128..127） */
    public void setWaveform(byte[] waveform) {
        if (waveform == null || waveform.length < 2) return;
        int bars = levels.length;
        int per = waveform.length / bars;
        for (int i = 0; i < bars; i++) {
            float sum = 0;
            for (int j = 0; j < per; j++) {
                int idx = i * per + j;
                if (idx < waveform.length) {
                    sum += Math.abs(waveform[idx]);
                }
            }
            levels[i] = Math.min(1f, sum / per / 127f);
        }
        invalidate();
    }

    public void startAnim() {
        animating = true;
        removeCallbacks(tick);
        postDelayed(tick, 80);
    }

    public void stopAnim() {
        animating = false;
        removeCallbacks(tick);
        for (int i = 0; i < levels.length; i++) levels[i] = 0;
        invalidate();
    }

    private void generateRandomLevels() {
        for (int i = 0; i < levels.length; i++) {
            float target = 0.15f + random.nextFloat() * 0.6f;
            levels[i] = levels[i] * 0.5f + target * 0.5f;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int barCount = levels.length;
        float gap = dp(4);
        float barWidth = (getWidth() - gap * (barCount - 1)) / (float) barCount;
        float maxH = getHeight() * 0.9f;
        for (int i = 0; i < barCount; i++) {
            float h = Math.max(dp(2), levels[i] * maxH);
            float left = i * (barWidth + gap);
            float right = left + barWidth;
            canvas.drawRoundRect(left, getHeight() - h, right, getHeight(), dp(1), dp(1), barPaint);
        }
    }
}
