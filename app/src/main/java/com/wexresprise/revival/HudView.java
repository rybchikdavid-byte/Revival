package com.wexresprise.revival;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import com.wexresprise.revival.gl.WorldRenderer;
import com.wexresprise.revival.net.RemotePlayer;
import com.wexresprise.revival.net.RevivalSession;

public class HudView extends View {

    public interface Callback {
        void onLookChanged();
    }

    private final World world;
    private final WorldRenderer renderer;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int joyId = -1;
    private float baseX;
    private float baseY;
    private float knobX;
    private float knobY;
    private float radius;

    private int lookId = -1;
    private float lastX;
    private float lastY;

    private Callback callback;

    public HudView(Context c, World world, WorldRenderer renderer) {
        super(c);
        this.world = world;
        this.renderer = renderer;
        radius = Ui.dp(c, 62);
        textPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(Ui.dp(c, 12));
    }

    public void setCallback(Callback c) {
        this.callback = c;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawNames(canvas);
        drawJoystick(canvas);
    }

    private void drawNames(Canvas canvas) {
        RevivalSession s = RevivalSession.current;
        if (s == null) return;
        for (RemotePlayer p : s.snapshot()) {
            if (p.id == s.localId) continue;
            float[] pos = renderer.project(p.x, p.y + 5.6f, p.z);
            if (pos == null) continue;
            if (pos[0] < -200 || pos[0] > getWidth() + 200) continue;
            textPaint.setColor(0xCC000000);
            canvas.drawText(p.nick, pos[0] + 1.5f, pos[1] + 1.5f, textPaint);
            textPaint.setColor(Color.WHITE);
            canvas.drawText(p.nick, pos[0], pos[1], textPaint);
        }
    }

    private void drawJoystick(Canvas canvas) {
        float bx = joyId == -1 ? Ui.dp(getContext(), 96) : baseX;
        float by = joyId == -1 ? getHeight() - Ui.dp(getContext(), 96) : baseY;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x33FFFFFF);
        canvas.drawCircle(bx, by, radius, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Ui.dp(getContext(), 2));
        paint.setColor(0x66FFFFFF);
        canvas.drawCircle(bx, by, radius, paint);

        float kx = bx;
        float ky = by;
        if (joyId != -1) {
            float dx = knobX - bx;
            float dy = knobY - by;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len > radius) {
                dx = dx / len * radius;
                dy = dy / len * radius;
            }
            kx = bx + dx;
            ky = by + dy;
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x88FFFFFF);
        canvas.drawCircle(kx, ky, radius * 0.42f, paint);
        paint.setColor(0xFFFFFFFF);
        canvas.drawCircle(kx, ky, radius * 0.30f, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int action = e.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                int index = e.getActionIndex();
                int id = e.getPointerId(index);
                float x = e.getX(index);
                float y = e.getY(index);
                if (joyId == -1 && x < getWidth() * 0.45f) {
                    joyId = id;
                    baseX = x;
                    baseY = y;
                    knobX = x;
                    knobY = y;
                    updateMove();
                } else if (lookId == -1) {
                    lookId = id;
                    lastX = x;
                    lastY = y;
                }
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                for (int i = 0; i < e.getPointerCount(); i++) {
                    int id = e.getPointerId(i);
                    if (id == joyId) {
                        knobX = e.getX(i);
                        knobY = e.getY(i);
                        updateMove();
                    } else if (id == lookId) {
                        float x = e.getX(i);
                        float y = e.getY(i);
                        world.camYaw -= (x - lastX) * 0.006f;
                        world.camPitch = Math.max(-0.05f, Math.min(0.95f,
                                world.camPitch + (y - lastY) * 0.004f));
                        lastX = x;
                        lastY = y;
                    }
                }
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL: {
                int index = e.getActionIndex();
                int id = e.getPointerId(index);
                if (id == joyId) {
                    joyId = -1;
                    world.moveX = 0;
                    world.moveZ = 0;
                }
                if (id == lookId) lookId = -1;
                break;
            }
            default:
                break;
        }
        invalidate();
        return true;
    }

    private void updateMove() {
        float dx = knobX - baseX;
        float dy = knobY - baseY;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1f) {
            world.moveX = 0;
            world.moveZ = 0;
            return;
        }
        float max = radius;
        float nx = dx / max;
        float ny = dy / max;
        if (len > max) {
            nx = dx / len;
            ny = dy / len;
        }
        world.moveX = nx;
        world.moveZ = ny;
        if (callback != null) callback.onLookChanged();
    }
}
