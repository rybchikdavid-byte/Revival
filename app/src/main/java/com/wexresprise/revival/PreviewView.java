package com.wexresprise.revival;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.view.View;

public class PreviewView extends View {

    private int kind;
    private int index;
    private Avatar base = new Avatar();

    public PreviewView(Context c) {
        super(c);
    }

    public void set(int kind, int index, Avatar base) {
        this.kind = kind;
        this.index = index;
        this.base = base == null ? new Avatar() : base;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        RectF box = new RectF(0, 0, getWidth(), getHeight());
        Avatar a = base.copy();
        switch (kind) {
            case Catalog.KIND_SHIRT:
                a.shirt = index;
                AvatarArt.draw(canvas, box, a, null);
                break;
            case Catalog.KIND_PANTS:
                a.pants = index;
                AvatarArt.draw(canvas, box, a, null);
                break;
            case Catalog.KIND_FACE:
                a.face = index;
                avatar(canvas, box, a);
                break;
            default:
                a.hair = index;
                avatar(canvas, box, a);
                break;
        }
    }

    private void avatar(Canvas canvas, RectF box, Avatar a) {
        float size = Math.min(box.width(), box.height());
        RectF r = new RectF(box.centerX() - size * 0.5f, box.centerY() - size * 0.5f,
                box.centerX() + size * 0.5f, box.centerY() + size * 0.5f);
        AvatarArt.drawHead(canvas, r, a);
    }
}
