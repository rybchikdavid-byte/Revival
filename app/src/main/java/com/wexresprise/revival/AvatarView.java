package com.wexresprise.revival;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.view.View;

public class AvatarView extends View {

    private Avatar avatar = new Avatar();
    private Bitmap custom;
    private boolean headOnly;

    public AvatarView(Context c) {
        super(c);
    }

    public void setAvatar(Avatar a, Bitmap customShirt) {
        this.avatar = a == null ? new Avatar() : a;
        this.custom = customShirt;
        invalidate();
    }

    public void setHeadOnly(boolean v) {
        this.headOnly = v;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        RectF box = new RectF(getPaddingLeft(), getPaddingTop(),
                getWidth() - getPaddingRight(), getHeight() - getPaddingBottom());
        if (box.width() <= 0 || box.height() <= 0) return;
        if (headOnly) {
            AvatarArt.drawHead(canvas, box, avatar);
        } else {
            AvatarArt.draw(canvas, box, avatar, custom);
        }
    }
}
