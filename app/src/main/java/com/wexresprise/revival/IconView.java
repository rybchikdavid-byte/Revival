package com.wexresprise.revival;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

public class IconView extends View {

    public enum Kind {BRICK, CHAT, PEOPLE, GEAR, HELP, JUMP, CLOSE, MORE, PALETTE, ARROW}

    private Kind kind = Kind.BRICK;
    private int color = Color.WHITE;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public IconView(Context c) {
        super(c);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.1f);
    }

    public void setKind(Kind k) {
        kind = k;
        invalidate();
    }

    public void setIconColor(int c) {
        color = c;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float s = Math.min(getWidth(), getHeight()) / 24f;
        canvas.save();
        canvas.translate((getWidth() - 24 * s) / 2f, (getHeight() - 24 * s) / 2f);
        canvas.scale(s, s);
        paint.setColor(color);

        switch (kind) {
            case BRICK:
                drawBrick(canvas);
                break;
            case CHAT:
                drawChat(canvas);
                break;
            case PEOPLE:
                drawPeople(canvas);
                break;
            case GEAR:
                drawGear(canvas);
                break;
            case HELP:
                drawHelp(canvas);
                break;
            case JUMP:
                drawJump(canvas);
                break;
            case CLOSE:
                drawClose(canvas);
                break;
            case MORE:
                drawMore(canvas);
                break;
            case PALETTE:
                drawPalette(canvas);
                break;
            default:
                drawArrow(canvas);
                break;
        }
        canvas.restore();
    }

    private void drawBrick(Canvas c) {
        c.save();
        c.rotate(-22, 12, 12);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.0f);
        RectF r = new RectF(4, 8, 20, 17);
        c.drawRoundRect(r, 2.4f, 2.4f, paint);
        paint.setStyle(Paint.Style.FILL);
        c.drawCircle(12, 6.2f, 3.4f, paint);
        paint.setStyle(Paint.Style.STROKE);
        c.restore();
    }

    private void drawChat(Canvas c) {
        RectF r = new RectF(3.5f, 4, 20.5f, 18.5f);
        c.drawRoundRect(r, 3.4f, 3.4f, paint);
        paint.setStrokeWidth(1.9f);
        c.drawLine(7.5f, 9.4f, 16.5f, 9.4f, paint);
        c.drawLine(7.5f, 12.4f, 16.5f, 12.4f, paint);
        c.drawLine(7.5f, 15.4f, 13f, 15.4f, paint);
    }

    private void drawPeople(Canvas c) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.9f);
        c.drawCircle(9.5f, 8.2f, 3.1f, paint);
        RectF body = new RectF(3.6f, 13.2f, 15.4f, 21.6f);
        c.drawArc(body, 180, 180, false, paint);
        c.drawCircle(17.2f, 9.6f, 2.3f, paint);
        RectF body2 = new RectF(13.4f, 14.4f, 21.4f, 21.6f);
        c.drawArc(body2, 195, 165, false, paint);
    }

    private void drawGear(Canvas c) {
        c.drawCircle(12, 12, 5.2f, paint);
        paint.setStrokeWidth(2.4f);
        for (int i = 0; i < 8; i++) {
            double a = Math.PI / 4 * i;
            float x1 = (float) (12 + Math.cos(a) * 7.2);
            float y1 = (float) (12 + Math.sin(a) * 7.2);
            float x2 = (float) (12 + Math.cos(a) * 9.4);
            float y2 = (float) (12 + Math.sin(a) * 9.4);
            c.drawLine(x1, y1, x2, y2, paint);
        }
    }

    private void drawHelp(Canvas c) {
        c.drawCircle(12, 12, 9f, paint);
        paint.setStrokeWidth(2.0f);
        Path p = new Path();
        p.moveTo(9.2f, 9.4f);
        p.cubicTo(9.6f, 6.4f, 15.2f, 6.4f, 15f, 9.4f);
        p.cubicTo(14.8f, 11.4f, 12f, 11.4f, 12f, 14f);
        c.drawPath(p, paint);
        paint.setStyle(Paint.Style.FILL);
        c.drawCircle(12f, 17f, 1.2f, paint);
        paint.setStyle(Paint.Style.STROKE);
    }

    private void drawJump(Canvas c) {
        paint.setStrokeWidth(2.6f);
        c.drawLine(12, 18.5f, 12, 8f, paint);
        Path p = new Path();
        p.moveTo(6.6f, 12.4f);
        p.lineTo(12f, 6.4f);
        p.lineTo(17.4f, 12.4f);
        c.drawPath(p, paint);
    }

    private void drawClose(Canvas c) {
        paint.setStrokeWidth(2.7f);
        c.drawLine(6.2f, 6.2f, 17.8f, 17.8f, paint);
        c.drawLine(17.8f, 6.2f, 6.2f, 17.8f, paint);
    }

    private void drawMore(Canvas c) {
        paint.setStyle(Paint.Style.FILL);
        c.drawCircle(6f, 12f, 1.9f, paint);
        c.drawCircle(12f, 12f, 1.9f, paint);
        c.drawCircle(18f, 12f, 1.9f, paint);
        paint.setStyle(Paint.Style.STROKE);
    }

    private void drawPalette(Canvas c) {
        c.drawCircle(12, 12, 8.4f, paint);
        paint.setStyle(Paint.Style.FILL);
        c.drawCircle(9.2f, 8.8f, 1.5f, paint);
        c.drawCircle(15.2f, 9.4f, 1.5f, paint);
        c.drawCircle(8.6f, 14.6f, 1.5f, paint);
        c.drawCircle(15.6f, 15f, 1.5f, paint);
        paint.setStyle(Paint.Style.STROKE);
    }

    private void drawArrow(Canvas c) {
        paint.setStrokeWidth(2.4f);
        c.drawLine(6f, 12f, 18f, 12f, paint);
        c.drawLine(13f, 7f, 18f, 12f, paint);
        c.drawLine(13f, 17f, 18f, 12f, paint);
    }
}
