package com.wexresprise.revival;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.View;

import java.util.Random;

public class BackdropView extends View {

    private YearTheme theme;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint particle = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] px = new float[22];
    private final float[] py = new float[22];
    private final float[] ps = new float[22];
    private final float[] pv = new float[22];
    private long start;

    public BackdropView(Context c, YearTheme theme) {
        super(c);
        this.theme = theme;
        Random r = new Random(7);
        for (int i = 0; i < px.length; i++) {
            px[i] = r.nextFloat();
            py[i] = r.nextFloat();
            ps[i] = 0.006f + r.nextFloat() * 0.022f;
            pv[i] = 0.004f + r.nextFloat() * 0.012f;
        }
    }

    public void setTheme(YearTheme theme) {
        this.theme = theme;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (start == 0) start = SystemClock.uptimeMillis();
        float t = (SystemClock.uptimeMillis() - start) / 1000f;

        paint.setShader(new LinearGradient(0, 0, getWidth(), getHeight(),
                theme.bg, theme.bg2, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        paint.setShader(null);

        // мягкое свечение акцента
        paint.setShader(new android.graphics.RadialGradient(getWidth() * 0.82f, getHeight() * 0.12f,
                Math.max(getWidth(), getHeight()) * 0.7f,
                AvatarArt.blend(theme.accent, theme.bg, 0.55f), theme.bg, Shader.TileMode.CLAMP));
        paint.setAlpha(120);
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        paint.setAlpha(255);
        paint.setShader(null);

        // лёгкая сетка
        paint.setColor(AvatarArt.blend(theme.bg, theme.accent, 0.10f));
        paint.setStrokeWidth(1f);
        int step = Ui.dp(getContext(), 42);
        for (int x = 0; x < getWidth(); x += step) {
            canvas.drawLine(x, 0, x, getHeight(), paint);
        }
        for (int y = 0; y < getHeight(); y += step) {
            canvas.drawLine(0, y, getWidth(), y, paint);
        }

        // плавающие кубики
        particle.setColor(AvatarArt.blend(theme.accent, theme.bg, 0.35f));
        for (int i = 0; i < px.length; i++) {
            float y = (py[i] - pv[i] * t) % 1f;
            if (y < 0) y += 1f;
            float size = ps[i] * getWidth();
            float x = px[i] * getWidth();
            particle.setAlpha(i % 3 == 0 ? 70 : 38);
            canvas.save();
            canvas.rotate(t * 14f + i * 23f, x, y * getHeight());
            RectF r = new RectF(x - size / 2, y * getHeight() - size / 2,
                    x + size / 2, y * getHeight() + size / 2);
            canvas.drawRoundRect(r, size * 0.18f, size * 0.18f, particle);
            canvas.restore();
        }
        particle.setAlpha(255);

        // нижняя тень
        paint.setShader(new LinearGradient(0, getHeight() * 0.72f, 0, getHeight(),
                Color.TRANSPARENT, AvatarArt.blend(theme.bg, Color.BLACK, 0.35f), Shader.TileMode.CLAMP));
        canvas.drawRect(0, getHeight() * 0.72f, getWidth(), getHeight(), paint);
        paint.setShader(null);

        postInvalidateOnAnimation();
    }
}
