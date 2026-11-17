package com.wexresprise.revival;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class Ui {

    private Ui() {
    }

    public static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    public static int sp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v,
                c.getResources().getDisplayMetrics()));
    }

    public static GradientDrawable round(int color, float radiusPx, int strokeColor, float strokePx) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radiusPx);
        if (strokePx > 0 && strokeColor != 0) {
            d.setStroke(Math.max(1, Math.round(strokePx)), strokeColor);
        }
        return d;
    }

    public static GradientDrawable roundRect(int color, float radiusPx, int strokeColor, float strokePx) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radiusPx);
        if (strokePx > 0 && strokeColor != 0) {
            d.setStroke(Math.max(1, Math.round(strokePx)), strokeColor);
        }
        return d;
    }

    public static TextView label(Context c, String text, float sizeSp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setIncludeFontPadding(false);
        return t;
    }

    public static TextView button(Context c, YearTheme theme, String text, boolean primary) {
        TextView t = label(c, text, 15, primary ? pickText(theme.accent) : theme.text, true);
        t.setGravity(Gravity.CENTER);
        int padH = dp(c, 18);
        int padV = dp(c, 13);
        t.setPadding(padH, padV, padH, padV);
        t.setClickable(true);
        t.setFocusable(true);
        t.setBackground(buttonDrawable(c, theme, primary));
        return t;
    }

    public static Drawable buttonDrawable(Context c, YearTheme theme, boolean primary) {
        if (primary) {
            return round(theme.accent, dp(c, theme.radius + 2), 0, 0);
        }
        GradientDrawable d = round(theme.chrome ? theme.panel2 : theme.panel, dp(c, theme.radius + 2),
                theme.chrome ? AvatarArt.blend(theme.panel, Color.WHITE, 0.22f) : AvatarArt.blend(theme.panel, theme.text, 0.10f),
                dp(c, 1));
        return d;
    }

    public static int pickText(int bg) {
        double lum = (0.299 * Color.red(bg) + 0.587 * Color.green(bg) + 0.114 * Color.blue(bg)) / 255.0;
        return lum > 0.62 ? 0xFF111318 : 0xFFFFFFFF;
    }

    public static void margins(View v, int l, int t, int r, int b) {
        if (v.getLayoutParams() instanceof android.view.ViewGroup.MarginLayoutParams) {
            ((android.view.ViewGroup.MarginLayoutParams) v.getLayoutParams()).setMargins(l, t, r, b);
        }
    }

    public static LinearLayout.LayoutParams llp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    public static LinearLayout.LayoutParams llp(int w, int h, float weight) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.weight = weight;
        return p;
    }

    public static FrameLayout.LayoutParams flp(int w, int h, int gravity) {
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(w, h);
        p.gravity = gravity;
        return p;
    }

    /** Кнопка старого стиля: с фаской и лёгким градиентом. */
    public static class BevelDrawable extends Drawable {
        private final int top;
        private final int bottom;
        private final int light;
        private final int dark;
        private final float radius;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();

        public BevelDrawable(int base, float radiusPx) {
            this.top = AvatarArt.blend(base, Color.WHITE, 0.16f);
            this.bottom = AvatarArt.blend(base, Color.BLACK, 0.20f);
            this.light = AvatarArt.blend(base, Color.WHITE, 0.42f);
            this.dark = AvatarArt.blend(base, Color.BLACK, 0.45f);
            this.radius = radiusPx;
        }

        @Override
        public void draw(Canvas canvas) {
            rect.set(getBounds());
            paint.setShader(new LinearGradient(0, rect.top, 0, rect.bottom, top, bottom, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(rect, radius, radius, paint);
            paint.setShader(null);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.5f);
            paint.setColor(light);
            canvas.drawLine(rect.left + radius, rect.top + 1, rect.right - radius, rect.top + 1, paint);
            paint.setColor(dark);
            canvas.drawLine(rect.left + radius, rect.bottom - 1, rect.right - radius, rect.bottom - 1, paint);
            paint.setStyle(Paint.Style.FILL);
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(android.graphics.ColorFilter colorFilter) {
            paint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return android.graphics.PixelFormat.OPAQUE;
        }
    }
}
