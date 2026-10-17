package com.redmusic.player.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

/** 色轮取色器：HSV 色相/饱和度圆盘 + 亮度条，取自定义主题色 */
public class ColorPickerDialog extends Dialog {

    public interface OnColorPick {
        void onPick(int color);
    }

    private float h, s = 0.6f, v = 1f;
    private OnColorPick listener;
    private View preview;

    public ColorPickerDialog(Context ctx, int initialColor, OnColorPick cb) {
        super(ctx);
        this.listener = cb;
        float[] hsv = new float[3];
        Color.colorToHSV(initialColor, hsv);
        h = hsv[0];
        s = hsv[1];
        v = hsv[2];

        Window w = getWindow();
        if (w != null) w.setBackgroundDrawableResource(android.R.color.transparent);

        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(ctx, 20);
        root.setPadding(pad, pad, pad, pad);

        final ColorWheelView wheel = new ColorWheelView(ctx);
        wheel.setLayoutParams(new LinearLayout.LayoutParams(dp(ctx, 260), dp(ctx, 260)));
        wheel.setOnColorListener((nh, ns) -> {
            h = nh;
            s = ns;
            refreshPreview();
        });
        root.addView(wheel);

        // 亮度条
        LinearLayout vRow = new LinearLayout(ctx);
        vRow.setOrientation(LinearLayout.HORIZONTAL);
        vRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView vLabel = new TextView(ctx);
        vLabel.setText("亮度");
        vLabel.setTextColor(0xFF888888);
        vLabel.setTextSize(14);
        vRow.addView(vLabel);
        SeekBar vBar = new SeekBar(ctx);
        vBar.setMax(100);
        vBar.setProgress((int) (v * 100));
        vBar.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        vBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean b) { v = p / 100f; refreshPreview(); }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });
        vRow.addView(vBar);
        root.addView(vRow);

        // 预览
        preview = new View(ctx);
        preview.setLayoutParams(new LinearLayout.LayoutParams(dp(ctx, 220), dp(ctx, 46)));
        ((LinearLayout.LayoutParams) preview.getLayoutParams()).setMargins(0, dp(ctx, 16), 0, 0);
        root.addView(preview);

        // 按钮
        LinearLayout btnRow = new LinearLayout(ctx);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        Button cancel = new Button(ctx);
        cancel.setText("取消");
        cancel.setTextSize(14);
        cancel.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        cancel.setOnClickListener(v -> dismiss());
        btnRow.addView(cancel);
        Button ok = new Button(ctx);
        ok.setText("确定");
        ok.setTextSize(14);
        ok.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        ok.setOnClickListener(v -> {
            if (listener != null) listener.onPick(currentColor());
            dismiss();
        });
        btnRow.addView(ok);
        root.addView(btnRow);

        setContentView(root);
        refreshPreview();
    }

    private int currentColor() {
        float[] hsv = {h, s, v};
        return Color.HSVToColor(hsv);
    }

    private void refreshPreview() {
        if (preview != null) preview.setBackgroundColor(currentColor());
    }

    private int dp(Context c, int v) {
        return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f);
    }

    /** HSV 色轮（色相圆周、饱和度径向），固定亮度 1，亮度由外部条控制 */
    static class ColorWheelView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int[] hueColors;
        private OnColorListener listener;
        private float curH = 0, curS = 0.6f;
        private int cx, cy, radius;
        private int wheelColor = 0xFFFFFFFF;

        interface OnColorListener { void onColor(float h, float s); }

        public ColorWheelView(Context context) {
            super(context);
            hueColors = new int[361];
            for (int i = 0; i <= 360; i++) hueColors[i] = Color.HSVToColor(new float[]{i, 1f, 1f});
        }

        public void setOnColorListener(OnColorListener l) { this.listener = l; }

        @Override
        protected void onSizeChanged(int w, int h, int ow, int oh) {
            cx = w / 2;
            cy = h / 2;
            radius = Math.min(w, h) / 2 - 4;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (radius <= 0) return;
            // 色相环（圆周彩色）
            SweepGradient sweep = new SweepGradient(cx, cy, hueColors, null);
            paint.setShader(sweep);
            canvas.drawCircle(cx, cy, radius, paint);
            // 饱和度：中心白（饱和 0）→ 边缘透明（保留色相）
            RadialGradient radial = new RadialGradient(
                    cx, cy, radius,
                    new int[]{0xFFFFFFFF, 0x00FFFFFF}, new float[]{0f, 1f},
                    Shader.TileMode.CLAMP);
            paint.setShader(radial);
            canvas.drawCircle(cx, cy, radius, paint);
            paint.setShader(null);
            // 当前色点
            float rad = (float) (Math.toRadians(curH));
            float x = cx + (float) Math.cos(rad) * radius * curS;
            float y = cy + (float) Math.sin(rad) * radius * curS;
            paint.setColor(0xFFFFFFFF);
            paint.setStrokeWidth(dp(3));
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawCircle(x, y, dp(10), paint);
            paint.setStyle(Paint.Style.FILL);
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            float dx = e.getX() - cx;
            float dy = e.getY() - cy;
            float dist = (float) Math.hypot(dx, dy);
            if (dist > radius) dist = radius;
            float s = dist / radius;
            if (s > 1f) s = 1f;
            float hDeg = (float) Math.toDegrees(Math.atan2(dy, dx));
            if (hDeg < 0) hDeg += 360;
            curH = hDeg;
            curS = s;
            if (listener != null) listener.onColor(curH, curS);
            invalidate();
            return true;
        }

        private int dp(int v) {
            return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
        }
    }
}
