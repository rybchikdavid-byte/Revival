package com.wexresprise.revival;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

import com.wexresprise.revival.net.RemotePlayer;
import com.wexresprise.revival.net.RevivalSession;
import com.wexresprise.revival.place.PlacePart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Запасной 2D-мир без OpenGL. Рисует плиту со студами, детали карты и
 * персонажей в перспективе. Нужен, если драйвер не даёт OpenGL: лучше
 * такой мир, чем вылет.
 */
public class FlatWorldView extends View {

    private final World world;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);

    private long lastTime;
    private double walkPhase;
    private float vy;

    private float camX;
    private float camZ;
    private float camY;

    private final List<PartDraw> partBuffer = new ArrayList<>();
    private final List<ActorDraw> actorBuffer = new ArrayList<>();

    public FlatWorldView(Context c, World world) {
        super(c);
        this.world = world;
        paint.setStyle(Paint.Style.FILL);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(Ui.dp(c, 12));
        text.setShadowLayer(Ui.dp(c, 2), 0, 1, 0xAA000000);
    }

    private float horizon() {
        return getHeight() * 0.40f;
    }

    private float focal() {
        return Math.max(1f, getHeight() * 0.90f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getWidth() <= 0 || getHeight() <= 0) return;
        try {
            step();
            drawWorld(canvas);
        } catch (Throwable t) {
            CrashLog.reportUi(getContext(), t);
        }
        if (isAttachedToWindow()) postInvalidateOnAnimation();
    }

    private void step() {
        long now = System.nanoTime();
        if (lastTime == 0) {
            lastTime = now;
            return;
        }
        float dt = (now - lastTime) / 1_000_000_000f;
        lastTime = now;
        if (dt > 0.08f) dt = 0.08f;
        if (dt <= 0f) return;

        float cam = world.camYaw;
        float fx = -(float) Math.sin(cam);
        float fz = -(float) Math.cos(cam);
        float rx = (float) Math.cos(cam);
        float rz = -(float) Math.sin(cam);

        float mx = world.moveX;
        float mz = world.moveZ;
        float len = (float) Math.sqrt(mx * mx + mz * mz);
        if (len > 1f) {
            mx /= len;
            mz /= len;
            len = 1f;
        }
        float speed = 19f;
        float vx = (fx * -mz + rx * mx) * speed;
        float vz = (fz * -mz + rz * mx) * speed;

        world.x = Math.max(-250f, Math.min(250f, world.x + vx * dt));
        world.z = Math.max(-250f, Math.min(250f, world.z + vz * dt));

        if (len > 0.08f) {
            world.yaw = (float) Math.toDegrees(Math.atan2(vx, vz));
            walkPhase += dt * 8.5 * len;
        }

        if (world.jumpRequested) {
            world.jumpRequested = false;
            if (world.y <= 0.001f) vy = 16f;
        }
        vy -= 44f * dt;
        world.y += vy * dt;
        if (world.y < 0) {
            world.y = 0;
            vy = 0;
        }
    }

    private void drawWorld(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        float hy = horizon();
        float f = focal();

        canvas.drawColor(0xFF8FC9EE);
        paint.setColor(0xFFC3C7CD);
        canvas.drawRect(0, hy, w, h, paint);
        paint.setColor(0xFFB0B5BC);
        canvas.drawRect(0, hy, w, hy + Math.max(1f, f / 220f), paint);

        float sin = (float) Math.sin(world.camYaw);
        float cos = (float) Math.cos(world.camYaw);
        camX = world.x + sin * 14f;
        camZ = world.z + cos * 14f;
        camY = world.y + 6.5f;

        drawStuds(canvas, f, hy);
        drawParts(canvas, f);

        actorBuffer.clear();
        RevivalSession s = RevivalSession.current;
        if (s != null) {
            for (RemotePlayer p : s.snapshot()) {
                if (p.id == s.localId) continue;
                float[] feet = project(p.x, p.y, p.z, f, hy);
                if (feet == null) continue;
                actorBuffer.add(new ActorDraw(p.x, p.y, p.z, p.shirt, p.pants, p.face, p.hair,
                        p.nick, feet[2]));
            }
        }
        float[] self = project(world.x, world.y, world.z, f, hy);
        actorBuffer.add(new ActorDraw(world.x, world.y, world.z, world.shirt, world.pants,
                world.face, world.hair, null, self == null ? 0f : self[2]));
        Collections.sort(actorBuffer, (a, b) -> Float.compare(b.depth, a.depth));
        for (ActorDraw a : actorBuffer) {
            drawActor(canvas, a, f, hy);
        }
    }

    private void drawStuds(Canvas canvas, float f, float hy) {
        float step = 4f;
        int range = 56;
        float cx = Math.round(world.x / step) * step;
        float cz = Math.round(world.z / step) * step;
        for (int ix = -range; ix <= range; ix += (int) step) {
            for (int iz = -range; iz <= range; iz += (int) step) {
                float wx = cx + ix;
                float wz = cz + iz;
                if (wx < -248f || wx > 248f || wz < -248f || wz > 248f) continue;
                float[] pt = project(wx, 0f, wz, f, hy);
                if (pt == null) continue;
                float sx = pt[0];
                float sy = pt[1];
                float d = pt[2];
                if (sx < -30f || sx > getWidth() + 30f || sy < hy - 2f
                        || sy > getHeight() + 40f || d > 120f) continue;
                float r = Math.min(24f, 2.0f * f / d);
                if (r < 0.6f) continue;
                paint.setColor(0xFFAEB2B8);
                canvas.drawCircle(sx, sy + Math.max(0.5f, r * 0.10f), r, paint);
                paint.setColor(0xFFD6DAE0);
                canvas.drawCircle(sx, sy, r * 0.92f, paint);
                paint.setColor(0xFFE2E6EC);
                canvas.drawCircle(sx - r * 0.16f, sy - r * 0.18f, r * 0.55f, paint);
            }
        }
    }

    private void drawParts(Canvas canvas, float f) {
        partBuffer.clear();
        List<PlacePart> parts = world.parts;
        int count = Math.min(parts.size(), 500);
        for (int i = 0; i < count; i++) {
            PlacePart p = parts.get(i);
            float base = p.y - p.sy / 2f;
            float[] pt = project(p.x, base, p.z, f, horizon());
            if (pt == null) continue;
            float d = pt[2];
            if (d > 140f) continue;
            float w = Math.max(1f, p.sx * f / d);
            float hh = Math.max(1f, p.sy * f / d);
            if (pt[1] < 0 || pt[0] + w < 0 || pt[0] - w > getWidth()) continue;
            partBuffer.add(new PartDraw(pt[0], pt[1], w, hh, p.color, d));
        }
        Collections.sort(partBuffer, (a, b) -> Float.compare(b.depth, a.depth));
        for (PartDraw p : partBuffer) {
            paint.setColor(p.color | 0xFF000000);
            canvas.drawRect(p.x - p.w / 2f, p.y - p.h, p.x + p.w / 2f, p.y, paint);
            paint.setColor(0x33FFFFFF);
            canvas.drawRect(p.x - p.w / 2f, p.y - p.h, p.x + p.w / 2f,
                    p.y - p.h + Math.max(1f, p.h * 0.08f), paint);
        }
    }

    private void drawActor(Canvas canvas, ActorDraw a, float f, float hy) {
        float[] feet = project(a.x, a.y, a.z, f, hy);
        float[] headTop = project(a.x, a.y + 4.8f, a.z, f, hy);
        if (feet == null || headTop == null) return;
        float h = feet[1] - headTop[1];
        if (h < 8f) return;
        float w = h * 0.38f;
        float headR = h * 0.135f;
        float top = headTop[1];
        float cx = feet[0];

        paint.setColor(0x33000000);
        canvas.drawOval(new RectF(cx - w * 0.7f, feet[1] - h * 0.035f,
                cx + w * 0.7f, feet[1] + h * 0.035f), paint);

        int pants = Catalog.pantsColor(a.pants);
        int shirt = Catalog.shirtColor(a.shirt);

        paint.setColor(pants);
        float legsTop = top + h * 0.66f;
        canvas.drawRect(cx - w * 0.42f, legsTop, cx - w * 0.06f, feet[1], paint);
        canvas.drawRect(cx + w * 0.06f, legsTop, cx + w * 0.42f, feet[1], paint);

        paint.setColor(shirt);
        float torsoTop = top + h * 0.26f;
        float torsoBottom = top + h * 0.68f;
        canvas.drawRect(cx - w * 0.5f, torsoTop, cx + w * 0.5f, torsoBottom, paint);
        canvas.drawRect(cx - w * 0.78f, torsoTop, cx - w * 0.5f, torsoBottom - h * 0.04f, paint);
        canvas.drawRect(cx + w * 0.5f, torsoTop, cx + w * 0.78f, torsoBottom - h * 0.04f, paint);

        float headCy = top + headR;
        paint.setColor(0xFFF2C24B);
        canvas.drawCircle(cx, headCy, headR, paint);

        int hair = Catalog.hairColor(a.hair);
        if (a.hair != 7) {
            paint.setColor(hair);
            canvas.drawArc(new RectF(cx - headR, headCy - headR, cx + headR, headCy + headR),
                    180f, 180f, true, paint);
        }

        paint.setColor(0xFF2A211C);
        canvas.drawCircle(cx - headR * 0.36f, headCy, Math.max(1f, headR * 0.11f), paint);
        canvas.drawCircle(cx + headR * 0.36f, headCy, Math.max(1f, headR * 0.11f), paint);
        canvas.drawRect(cx - headR * 0.28f, headCy + headR * 0.42f,
                cx + headR * 0.28f, headCy + headR * 0.42f + Math.max(1f, headR * 0.08f), paint);

        if (a.nick != null && !a.nick.isEmpty()) {
            text.setColor(0xCC000000);
            canvas.drawText(a.nick, cx + 1.5f, top - Ui.dp(getContext(), 4) + 1.5f, text);
            text.setColor(Color.WHITE);
            canvas.drawText(a.nick, cx, top - Ui.dp(getContext(), 4), text);
        }
    }

    private float[] project(float wx, float wy, float wz, float f, float hy) {
        float sin = (float) Math.sin(world.camYaw);
        float cos = (float) Math.cos(world.camYaw);
        float fx = -sin;
        float fz = -cos;
        float rx = cos;
        float rz = -sin;
        float dx = wx - camX;
        float dz = wz - camZ;
        float depth = dx * fx + dz * fz;
        if (depth < 0.40f) return null;
        float lateral = dx * rx + dz * rz;
        float sx = getWidth() / 2f + lateral * f / depth;
        float sy = hy + (camY - wy) * f / depth;
        return new float[]{sx, sy, depth};
    }

    private static class PartDraw {
        final float x;
        final float y;
        final float w;
        final float h;
        final int color;
        final float depth;

        PartDraw(float x, float y, float w, float h, int color, float depth) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.color = color;
            this.depth = depth;
        }
    }

    private static class ActorDraw {
        final float x;
        final float y;
        final float z;
        final int shirt;
        final int pants;
        final int face;
        final int hair;
        final String nick;
        final float depth;

        ActorDraw(float x, float y, float z, int shirt, int pants, int face, int hair,
                  String nick, float depth) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.shirt = shirt;
            this.pants = pants;
            this.face = face;
            this.hair = hair;
            this.nick = nick;
            this.depth = depth;
        }
    }
}
