package com.wexresprise.revival;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

public class GLSurfaceViewBridge extends GLSurfaceView {

    public GLSurfaceViewBridge(Context context) {
        super(context);
        init();
    }

    public GLSurfaceViewBridge(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setEGLContextClientVersion(2);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }
}
