package com.wexresprise.revival;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;

public final class AvatarArt {

    public static final int SKIN = 0xFFF2C24B;
    public static final int SKIN_DARK = 0xFFD8A63B;
    public static final int SHOE = 0xFF26292F;
    public static final int EYE = 0xFF1B1D22;

    private AvatarArt() {
    }

    public static void draw(Canvas c, RectF box, Avatar a, Bitmap customShirt) {
        float u = Math.min(box.width() / 4.6f, box.height() / 6.0f);
        if (u <= 0) return;
        float cx = box.centerX();
        float top = box.top + (box.height() - 5.7f * u) / 2f;

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        // тень
        p.setColor(0x33000000);
        c.drawOval(new RectF(cx - 1.6f * u, top + 5.30f * u, cx + 1.6f * u, top + 5.75f * u), p);

        int pants = Catalog.pantsColor(a.pants);
        int shirt = Catalog.shirtColor(a.shirt);

        // ноги
        drawLeg(c, cx - 0.95f * u, top + 3.38f * u, 0.92f * u, 2.0f * u, pants, u);
        drawLeg(c, cx + 0.03f * u, top + 3.38f * u, 0.92f * u, 2.0f * u, pants, u);

        // руки
        drawArm(c, cx - 1.95f * u, top + 1.38f * u, 0.93f * u, 2.0f * u, shirt, u);
        drawArm(c, cx + 1.02f * u, top + 1.38f * u, 0.93f * u, 2.0f * u, shirt, u);

        // торс
        RectF torso = new RectF(cx - 1.0f * u, top + 1.38f * u, cx + 1.0f * u, top + 3.38f * u);
        p.setColor(shirt);
        c.drawRoundRect(torso, 0.10f * u, 0.10f * u, p);

        c.save();
        c.clipRect(torso);
        if (customShirt != null) {
            drawBitmapCover(c, customShirt, torso);
        } else {
            drawPattern(c, torso, a.shirt, shirt, u);
        }
        // мягкая тень по краю
        p.setColor(0x1A000000);
        c.drawRect(torso.left, torso.top, torso.left + 0.22f * u, torso.bottom, p);
        c.restore();

        // голова
        RectF head = new RectF(cx - 0.68f * u, top + 0.05f * u, cx + 0.68f * u, top + 1.37f * u);
        p.setColor(SKIN);
        c.drawRoundRect(head, 0.12f * u, 0.12f * u, p);
        p.setColor(SKIN_DARK);
        c.drawRect(head.right - 0.16f * u, head.top + 0.08f * u, head.right - 0.05f * u, head.bottom - 0.06f * u, p);

        drawFace(c, head, a.face, u);
        drawHair(c, head, a.hair, u);
        drawFace(c, head, a.face, u);
    }

    /** Крупный портрет: голова, лицо и волосы. */
    public static void drawHead(Canvas c, RectF box, Avatar a) {
        float u = Math.min(box.width(), box.height()) / 1.55f;
        RectF head = new RectF(box.centerX() - 0.68f * u, box.centerY() - 0.66f * u,
                box.centerX() + 0.68f * u, box.centerY() + 0.66f * u);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(SKIN);
        c.drawRoundRect(head, 0.12f * u, 0.12f * u, p);
        p.setColor(SKIN_DARK);
        c.drawRect(head.right - 0.16f * u, head.top + 0.08f * u, head.right - 0.05f * u, head.bottom - 0.06f * u, p);
        drawFace(c, head, a.face, u);
        drawHair(c, head, a.hair, u);
        drawFace(c, head, a.face, u);
    }

    /** Плоская иконка предмета одежды. */
    public static void drawShirtIcon(Canvas c, RectF box, int index) {
        float u = Math.min(box.width(), box.height()) / 2.4f;
        float cx = box.centerX();
        float top = box.centerY() - 1.0f * u;
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        int base = Catalog.shirtColor(index);
        p.setColor(AvatarArt.blend(base, Color.BLACK, 0.12f));
        c.drawRoundRect(new RectF(cx - 1.0f * u, top, cx - 0.62f * u, top + 1.55f * u), 0.18f * u, 0.18f * u, p);
        c.drawRoundRect(new RectF(cx + 0.62f * u, top, cx + 1.0f * u, top + 1.55f * u), 0.18f * u, 0.18f * u, p);
        RectF torso = new RectF(cx - 0.72f * u, top, cx + 0.72f * u, top + 2.0f * u);
        p.setColor(base);
        c.drawRoundRect(torso, 0.14f * u, 0.14f * u, p);
        c.save();
        c.clipRect(torso);
        drawPattern(c, torso, index, base, u);
        c.restore();
    }

    private static void drawLeg(Canvas c, float x, float y, float w, float h, int color, float u) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        RectF r = new RectF(x, y, x + w, y + h - 0.22f * u);
        p.setColor(color);
        c.drawRoundRect(r, 0.07f * u, 0.07f * u, p);
        RectF shoe = new RectF(x - 0.04f * u, y + h - 0.26f * u, x + w + 0.04f * u, y + h);
        p.setColor(SHOE);
        c.drawRoundRect(shoe, 0.06f * u, 0.06f * u, p);
    }

    private static void drawArm(Canvas c, float x, float y, float w, float h, int shirt, float u) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        RectF sleeve = new RectF(x, y, x + w, y + h * 0.46f);
        p.setColor(shirt);
        c.drawRoundRect(sleeve, 0.07f * u, 0.07f * u, p);
        RectF skin = new RectF(x, y + h * 0.46f, x + w, y + h);
        p.setColor(SKIN);
        c.drawRoundRect(skin, 0.07f * u, 0.07f * u, p);
    }

    private static void drawBitmapCover(Canvas c, Bitmap bmp, RectF dst) {
        float scale = Math.max(dst.width() / bmp.getWidth(), dst.height() / bmp.getHeight());
        float w = bmp.getWidth() * scale;
        float h = bmp.getHeight() * scale;
        float left = dst.centerX() - w / 2f;
        float top = dst.centerY() - h / 2f;
        Rect src = new Rect(0, 0, bmp.getWidth(), bmp.getHeight());
        RectF d = new RectF(left, top, left + w, top + h);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setFilterBitmap(true);
        c.drawBitmap(bmp, src, d, p);
    }

    private static void drawPattern(Canvas c, RectF r, int index, int base, float u) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        int light = blend(base, Color.WHITE, 0.35f);
        int dark = blend(base, Color.BLACK, 0.25f);
        switch (index % 8) {
            case 1: {
                p.setColor(light);
                float step = r.width() / 5f;
                for (int i = 0; i < 5; i += 2) {
                    c.drawRect(r.left + i * step, r.top, r.left + (i + 1) * step, r.bottom, p);
                }
                break;
            }
            case 2: {
                p.setColor(light);
                Path star = new Path();
                float cx = r.centerX();
                float cy = r.centerY();
                float R = r.width() * 0.34f;
                float r2 = R * 0.44f;
                for (int i = 0; i < 10; i++) {
                    double ang = -Math.PI / 2 + i * Math.PI / 5;
                    float rad = (i % 2 == 0) ? R : r2;
                    float x = cx + (float) Math.cos(ang) * rad;
                    float y = cy + (float) Math.sin(ang) * rad;
                    if (i == 0) star.moveTo(x, y); else star.lineTo(x, y);
                }
                star.close();
                c.drawPath(star, p);
                break;
            }
            case 3: {
                p.setColor(light);
                float s = r.width() * 0.20f;
                for (int iy = 0; iy < 3; iy++) {
                    for (int ix = 0; ix < 2; ix++) {
                        float x = r.left + r.width() * (0.25f + ix * 0.5f);
                        float y = r.top + r.height() * (0.2f + iy * 0.3f);
                        Path d = new Path();
                        d.moveTo(x, y - s / 2);
                        d.lineTo(x + s / 2, y);
                        d.lineTo(x, y + s / 2);
                        d.lineTo(x - s / 2, y);
                        d.close();
                        c.drawPath(d, p);
                    }
                }
                break;
            }
            case 4: {
                p.setColor(light);
                Path bolt = new Path();
                float cx = r.centerX();
                float cy = r.centerY();
                float s = r.width() * 0.22f;
                bolt.moveTo(cx + s * 0.35f, cy - s);
                bolt.lineTo(cx - s * 0.55f, cy + s * 0.15f);
                bolt.lineTo(cx - s * 0.02f, cy + s * 0.15f);
                bolt.lineTo(cx - s * 0.35f, cy + s);
                bolt.lineTo(cx + s * 0.6f, cy - s * 0.2f);
                bolt.lineTo(cx + s * 0.05f, cy - s * 0.2f);
                bolt.close();
                c.drawPath(bolt, p);
                break;
            }
            case 5: {
                float step = r.width() / 4f;
                p.setColor(dark);
                for (int iy = 0; iy < 4; iy++) {
                    for (int ix = 0; ix < 4; ix++) {
                        if ((ix + iy) % 2 == 0) {
                            c.drawRect(r.left + ix * step, r.top + iy * step,
                                    r.left + (ix + 1) * step, r.top + (iy + 1) * step, p);
                        }
                    }
                }
                break;
            }
            case 6: {
                p.setColor(blend(base, Color.WHITE, 0.75f));
                float cx = r.centerX();
                float cy = r.centerY();
                float s = r.width() * 0.26f;
                Path h = new Path();
                h.moveTo(cx, cy + s * 0.75f);
                h.cubicTo(cx - s * 1.5f, cy - s * 0.15f, cx - s * 0.65f, cy - s * 1.1f, cx, cy - s * 0.35f);
                h.cubicTo(cx + s * 0.65f, cy - s * 1.1f, cx + s * 1.5f, cy - s * 0.15f, cx, cy + s * 0.75f);
                h.close();
                c.drawPath(h, p);
                break;
            }
            case 7: {
                float s = r.width() / 8f;
                int[] cells = {0, 2, 5, 7, 9, 11, 14, 16, 18, 21, 23, 26, 28, 31};
                for (int i = 0; i < cells.length; i++) {
                    int cell = cells[i];
                    int ix = cell % 4;
                    int iy = cell / 4;
                    p.setColor(i % 2 == 0 ? light : dark);
                    c.drawRect(r.left + ix * s * 2, r.top + iy * s * 1.5f,
                            r.left + (ix + 1) * s * 2, r.top + (iy + 1) * s * 1.5f, p);
                }
                break;
            }
            default:
                break;
        }
    }

    private static void drawFace(Canvas c, RectF head, int index, float u) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(EYE);
        float eyeY = head.top + head.height() * 0.42f;
        float ex1 = head.left + head.width() * 0.28f;
        float ex2 = head.left + head.width() * 0.72f;
        float ew = 0.15f * u;
        float eh = 0.19f * u;
        float mouthY = head.top + head.height() * 0.72f;

        switch (index % 8) {
            case 0: {
                c.drawRoundRect(new RectF(ex1 - ew / 2, eyeY - eh / 2, ex1 + ew / 2, eyeY + eh / 2), ew / 2, ew / 2, p);
                c.drawRoundRect(new RectF(ex2 - ew / 2, eyeY - eh / 2, ex2 + ew / 2, eyeY + eh / 2), ew / 2, ew / 2, p);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(0.06f * u);
                p.setStrokeCap(Paint.Cap.ROUND);
                RectF m = new RectF(head.centerX() - 0.26f * u, mouthY - 0.16f * u, head.centerX() + 0.26f * u, mouthY + 0.12f * u);
                c.drawArc(m, 20, 140, false, p);
                p.setStyle(Paint.Style.FILL);
                break;
            }
            case 1: {
                c.drawRoundRect(new RectF(ex1 - ew / 2, eyeY - eh / 2, ex1 + ew / 2, eyeY + eh / 2), ew / 2, ew / 2, p);
                c.drawRoundRect(new RectF(ex2 - ew / 2, eyeY - eh / 2, ex2 + ew / 2, eyeY + eh / 2), ew / 2, ew / 2, p);
                RectF m = new RectF(head.centerX() - 0.34f * u, mouthY - 0.10f * u, head.centerX() + 0.34f * u, mouthY + 0.30f * u);
                c.drawRoundRect(m, 0.08f * u, 0.08f * u, p);
                break;
            }
            case 2: {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(0.055f * u);
                p.setStrokeCap(Paint.Cap.ROUND);
                c.drawLine(ex1 - ew / 2, eyeY, ex1 + ew / 2, eyeY, p);
                p.setStyle(Paint.Style.FILL);
                c.drawRoundRect(new RectF(ex2 - ew / 2, eyeY - eh / 2, ex2 + ew / 2, eyeY + eh / 2), ew / 2, ew / 2, p);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(0.06f * u);
                RectF m = new RectF(head.centerX() - 0.24f * u, mouthY - 0.14f * u, head.centerX() + 0.28f * u, mouthY + 0.14f * u);
                c.drawArc(m, 15, 150, false, p);
                p.setStyle(Paint.Style.FILL);
                break;
            }
            case 3: {
                c.drawRoundRect(new RectF(ex1 - ew / 2, eyeY - eh / 2, ex1 + ew / 2, eyeY + eh / 2), ew / 2, ew / 2, p);
                c.drawRoundRect(new RectF(ex2 - ew / 2, eyeY - eh / 2, ex2 + ew / 2, eyeY + eh / 2), ew / 2, ew / 2, p);
                RectF m = new RectF(head.centerX() - 0.26f * u, mouthY - 0.14f * u, head.centerX() + 0.26f * u, mouthY + 0.16f * u);
                c.drawRoundRect(m, 0.06f * u, 0.06f * u, p);
                p.setColor(0xFFE06A7A);
                c.drawRoundRect(new RectF(head.centerX() - 0.10f * u, mouthY + 0.02f * u,
                        head.centerX() + 0.10f * u, mouthY + 0.22f * u), 0.05f * u, 0.05f * u, p);
                break;
            }
            case 4: {
                p.setStrokeWidth(0.055f * u);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeCap(Paint.Cap.ROUND);
                c.drawLine(ex1 - ew / 2, eyeY - eh / 2 - 0.04f * u, ex1 + ew / 2, eyeY - eh / 2 + 0.02f * u, p);
                c.drawLine(ex2 - ew / 2, eyeY - eh / 2 + 0.02f * u, ex2 + ew / 2, eyeY - eh / 2 - 0.04f * u, p);
                p.setStyle(Paint.Style.FILL);
                c.drawRoundRect(new RectF(ex1 - ew / 2, eyeY, ex1 + ew / 2, eyeY + eh * 0.7f), ew / 2, ew / 2, p);
                c.drawRoundRect(new RectF(ex2 - ew / 2, eyeY, ex2 + ew / 2, eyeY + eh * 0.7f), ew / 2, ew / 2, p);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(0.06f * u);
                RectF m = new RectF(head.centerX() - 0.26f * u, mouthY + 0.02f * u, head.centerX() + 0.26f * u, mouthY + 0.26f * u);
                c.drawArc(m, 200, 140, false, p);
                p.setStyle(Paint.Style.FILL);
                break;
            }
            case 5: {
                p.setColor(0xFF14161A);
                c.drawRoundRect(new RectF(head.left + 0.10f * u, eyeY - 0.16f * u,
                        head.right - 0.10f * u, eyeY + 0.12f * u), 0.06f * u, 0.06f * u, p);
                p.setColor(0xFF3A4048);
                c.drawRect(head.left + 0.14f * u, eyeY - 0.10f * u, head.centerX() - 0.06f * u, eyeY - 0.02f * u, p);
                p.setColor(EYE);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(0.06f * u);
                p.setStrokeCap(Paint.Cap.ROUND);
                RectF m = new RectF(head.centerX() - 0.24f * u, mouthY - 0.12f * u, head.centerX() + 0.24f * u, mouthY + 0.13f * u);
                c.drawArc(m, 15, 150, false, p);
                p.setStyle(Paint.Style.FILL);
                break;
            }
            case 6: {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(0.055f * u);
                p.setStrokeCap(Paint.Cap.ROUND);
                c.drawLine(ex1 - ew / 2, eyeY, ex1 + ew / 2, eyeY, p);
                c.drawLine(ex2 - ew / 2, eyeY, ex2 + ew / 2, eyeY, p);
                c.drawLine(head.centerX() - 0.16f * u, mouthY + 0.04f * u, head.centerX() + 0.16f * u, mouthY + 0.04f * u, p);
                p.setStyle(Paint.Style.FILL);
                break;
            }
            default: {
                p.setColor(0xFF2EE6C8);
                c.drawRect(ex1 - ew / 2, eyeY - eh / 2, ex1 + ew / 2, eyeY + eh / 2, p);
                c.drawRect(ex2 - ew / 2, eyeY - eh / 2, ex2 + ew / 2, eyeY + eh / 2, p);
                p.setColor(0xFF1B1D22);
                float w = 0.06f * u;
                for (int i = 0; i < 4; i++) {
                    c.drawRect(head.centerX() - 0.26f * u + i * 0.15f * u, mouthY - 0.12f * u,
                            head.centerX() - 0.26f * u + i * 0.15f * u + w, mouthY + 0.12f * u, p);
                }
                break;
            }
        }
    }

    private static void drawHair(Canvas c, RectF head, int index, float u) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        int color = Catalog.hairColor(index);
        p.setColor(color);
        if (index == 7) return;

        switch (index % 8) {
            case 0: {
                RectF cap = new RectF(head.left - 0.04f * u, head.top - 0.04f * u,
                        head.right + 0.04f * u, head.top + head.height() * 0.40f);
                c.drawRoundRect(cap, 0.14f * u, 0.14f * u, p);
                break;
            }
            case 1: {
                float base = head.top + head.height() * 0.30f;
                Path sp = new Path();
                int n = 5;
                for (int i = 0; i < n; i++) {
                    float x = head.left + head.width() * (i / (float) n);
                    float x2 = head.left + head.width() * ((i + 1) / (float) n);
                    sp.moveTo(x, base);
                    sp.lineTo((x + x2) / 2f, head.top - 0.12f * u);
                    sp.lineTo(x2, base);
                }
                c.drawPath(sp, p);
                c.drawRect(head.left, base - 0.06f * u, head.right, base + 0.05f * u, p);
                break;
            }
            case 2: {
                c.drawRoundRect(new RectF(head.left - 0.08f * u, head.top - 0.02f * u,
                        head.right + 0.08f * u, head.top + head.height() * 0.50f), 0.16f * u, 0.16f * u, p);
                c.drawRect(head.left - 0.08f * u, head.top + head.height() * 0.16f * u,
                        head.left + 0.02f * u, head.top + head.height() * 0.72f, p);
                c.drawRect(head.right - 0.02f * u, head.top + head.height() * 0.16f * u,
                        head.right + 0.08f * u, head.top + head.height() * 0.72f, p);
                break;
            }
            case 3: {
                Path s = new Path();
                s.moveTo(head.left - 0.05f * u, head.top + head.height() * 0.34f);
                s.lineTo(head.left + head.width() * 0.20f, head.top - 0.10f * u);
                s.lineTo(head.right + 0.04f * u, head.top + head.height() * 0.10f);
                s.lineTo(head.right - 0.02f * u, head.top + head.height() * 0.36f);
                s.close();
                c.drawPath(s, p);
                break;
            }
            case 4: {
                p.setColor(0xFF2E5C8A);
                c.drawRoundRect(new RectF(head.left - 0.06f * u, head.top - 0.02f * u,
                        head.right + 0.06f * u, head.top + head.height() * 0.36f), 0.14f * u, 0.14f * u, p);
                RectF brim = new RectF(head.left - 0.02f * u, head.top + head.height() * 0.30f,
                        head.right + 0.42f * u, head.top + head.height() * 0.42f);
                c.drawRoundRect(brim, 0.06f * u, 0.06f * u, p);
                break;
            }
            case 5: {
                c.drawCircle(head.centerX(), head.top + head.height() * 0.30f, head.width() * 0.66f, p);
                break;
            }
            default: {
                Path m = new Path();
                m.moveTo(head.centerX() - 0.10f * u, head.top + head.height() * 0.30f);
                m.lineTo(head.centerX() - 0.05f * u, head.top - 0.20f * u);
                m.lineTo(head.centerX() + 0.06f * u, head.top - 0.16f * u);
                m.lineTo(head.centerX() + 0.11f * u, head.top + head.height() * 0.30f);
                m.close();
                c.drawPath(m, p);
                break;
            }
        }
    }

    public static int blend(int a, int b, float f) {
        float inv = 1f - f;
        int ra = (int) (Color.red(a) * inv + Color.red(b) * f);
        int ga = (int) (Color.green(a) * inv + Color.green(b) * f);
        int ba = (int) (Color.blue(a) * inv + Color.blue(b) * f);
        return Color.rgb(clamp(ra), clamp(ga), clamp(ba));
    }

    private static int clamp(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }
}
