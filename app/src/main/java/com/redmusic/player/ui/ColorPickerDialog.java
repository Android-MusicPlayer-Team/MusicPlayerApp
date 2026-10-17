package com.redmusic.player.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.SweepGradient;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** 色轮取色器：环形色相环 + 中心方形明度/饱和度面板（HSV），取自定义主题色 */
public class ColorPickerDialog extends Dialog {

    public interface OnColorPick { void onPick(int color); }

    private float h, s = 0.6f, v = 1f;
    private OnColorPick listener;
    private View preview;
    private TextView hText, sText, vText;

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

        int pad = dp(ctx, 20);
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        // 方形 明度/饱和度 面板（先定义，供下方色环回调引用）
        final SVPanelView sv = new SVPanelView(ctx);
        sv.setLayoutParams(new LinearLayout.LayoutParams(dp(ctx, 250), dp(ctx, 160)));
        ((LinearLayout.LayoutParams) sv.getLayoutParams()).setMargins(0, dp(ctx, 10), 0, 0);
        sv.setHSV(h, s, v);
        sv.setOnChange((ns, nv) -> {
            s = ns;
            v = nv;
            invalidateAll();
        });

        // 环形色相环（显示在明度/饱和度面板上方）
        final HueRingView ring = new HueRingView(ctx);
        ring.setLayoutParams(new LinearLayout.LayoutParams(dp(ctx, 250), dp(ctx, 250)));
        ring.setHue(h);
        ring.setOnHueChange(ht -> {
            h = ht;
            sv.setHSV(h, s, v); // 拖动色环，下方明度/饱和度面板随新色相变色
            invalidateAll();
        });
        root.addView(ring);
        root.addView(sv);

        // H/S/V 数值 + 预览
        LinearLayout info = new LinearLayout(ctx);
        info.setOrientation(LinearLayout.HORIZONTAL);
        info.setGravity(android.view.Gravity.CENTER_VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        ((LinearLayout.LayoutParams) info.getLayoutParams()).setMargins(0, dp(ctx, 12), 0, 0);

        hText = text(ctx, "H:0");
        sText = text(ctx, "S:0");
        vText = text(ctx, "V:0");
        info.addView(hText);
        info.addView(sText);
        info.addView(vText);

        preview = new View(ctx);
        preview.setLayoutParams(new LinearLayout.LayoutParams(dp(ctx, 70), dp(ctx, 40)));
        ((LinearLayout.LayoutParams) preview.getLayoutParams()).setMargins(dp(ctx, 12), 0, 0, 0);
        info.addView(preview);
        root.addView(info);

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
        invalidateAll();
    }

    private TextView text(Context ctx, String t) {
        TextView tv = new TextView(ctx);
        tv.setText(t);
        tv.setTextColor(0xFF888888);
        tv.setTextSize(13);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        return tv;
    }

    private void invalidateAll() {
        if (preview != null) preview.setBackgroundColor(currentColor());
        if (hText != null) hText.setText("H:" + Math.round(h));
        if (sText != null) sText.setText("S:" + Math.round(s * 100));
        if (vText != null) vText.setText("V:" + Math.round(v * 100));
    }

    private int currentColor() {
        return Color.HSVToColor(new float[]{h, s, v});
    }

    private int dp(Context c, int v) {
        return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f);
    }

    /** 环形色相轮 */
    static class HueRingView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint marker = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int[] hueColors;
        private float hue = 0f;
        private int cx, cy, radius, innerRadius;
        private OnHueChange listener;

        interface OnHueChange { void onHue(float h); }

        public HueRingView(Context context) {
            super(context);
            hueColors = new int[361];
            for (int i = 0; i <= 360; i++) hueColors[i] = Color.HSVToColor(new float[]{i, 1f, 1f});
            marker.setStyle(Paint.Style.STROKE);
            marker.setStrokeWidth(3f);
            marker.setColor(0xFFFFFFFF);
        }

        public void setOnHueChange(OnHueChange l) { this.listener = l; }
        public void setHue(float h) { this.hue = h; invalidate(); }

        @Override
        protected void onSizeChanged(int w, int h, int ow, int oh) {
            cx = w / 2;
            cy = h / 2;
            radius = Math.min(w, h) / 2 - 6;
            innerRadius = (int) (radius * 0.8f);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (radius <= 0) return;
            // 色相环（圆环）
            paint.setShader(new SweepGradient(cx, cy, hueColors, null));
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(cx, cy, radius, paint);
            paint.setShader(null);
            // 中心挖空：用页面背景色填充（形成色环）
            paint.setColor(com.redmusic.player.util.ThemeManager.backgroundColor(getContext()));
            canvas.drawCircle(cx, cy, innerRadius, paint);
            // 当前色相标记
            float rad = (float) Math.toRadians(hue);
            float mx = cx + (float) Math.cos(rad) * (radius + innerRadius) / 2f;
            float my = cy + (float) Math.sin(rad) * (radius + innerRadius) / 2f;
            marker.setStrokeWidth(4f);
            canvas.drawCircle(mx, my, 8f, marker);
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            float dx = e.getX() - cx;
            float dy = e.getY() - cy;
            float dist = (float) Math.hypot(dx, dy);
            if (dist < innerRadius || dist > radius) return true; // 只响应色环区域
            float hDeg = (float) Math.toDegrees(Math.atan2(dy, dx));
            if (hDeg < 0) hDeg += 360;
            hue = hDeg;
            if (listener != null) listener.onHue(hue);
            invalidate();
            return true;
        }
    }

    /** 方形 明度/饱和度 面板 */
    static class SVPanelView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint cursor = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float hue = 0f, sat = 0.6f, val = 1f;
        private OnChange listener;

        interface OnChange { void onChange(float s, float v); }

        public SVPanelView(Context context) {
            super(context);
            cursor.setStyle(Paint.Style.STROKE);
            cursor.setStrokeWidth(3f);
            cursor.setColor(0xFFFFFFFF);
        }

        public void setHSV(float h, float s, float v) { this.hue = h; this.sat = s; this.val = v; invalidate(); }
        public void setOnChange(OnChange l) { this.listener = l; }

        @Override
        protected void onDraw(Canvas canvas) {
            int w = getWidth(), h = getHeight();
            if (w <= 0 || h <= 0) return;
            // 底色：当前色相（s=1,v=1）
            paint.setColor(Color.HSVToColor(new float[]{hue, 1f, 1f}));
            canvas.drawRect(0, 0, w, h, paint);
            // 横向：白 → 透明（饱和度）
            paint.setShader(new LinearGradient(0, 0, w, 0, new int[]{0xFFFFFFFF, 0x00FFFFFF}, null, android.graphics.Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, paint);
            // 纵向：透明 → 黑（明度）
            paint.setShader(new LinearGradient(0, 0, 0, h, new int[]{0x00FFFFFF, 0xFF000000}, null, android.graphics.Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, paint);
            paint.setShader(null);
            // 光标
            float x = sat * w, y = (1f - val) * h;
            cursor.setStrokeWidth(3f);
            canvas.drawCircle(x, y, 10f, cursor);
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            float s = Math.max(0f, Math.min(1f, e.getX() / getWidth()));
            float v = Math.max(0f, Math.min(1f, 1f - e.getY() / getHeight()));
            sat = s;
            val = v;
            if (listener != null) listener.onChange(sat, val);
            invalidate();
            return true;
        }
    }
}
