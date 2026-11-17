package com.wexresprise.revival.gl;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import com.wexresprise.revival.Catalog;
import com.wexresprise.revival.World;
import com.wexresprise.revival.net.RemotePlayer;
import com.wexresprise.revival.net.RevivalSession;
import com.wexresprise.revival.place.PlacePart;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class WorldRenderer implements GLSurfaceView.Renderer {

    private static final String VS =
            "uniform mat4 uMvp;\n" +
                    "attribute vec3 aPos;\n" +
                    "attribute vec3 aNormal;\n" +
                    "varying float vShade;\n" +
                    "void main() {\n" +
                    "  gl_Position = uMvp * vec4(aPos, 1.0);\n" +
                    "  vec3 n = normalize(aNormal);\n" +
                    "  float d = max(dot(n, normalize(vec3(0.42, 0.86, 0.31))), 0.0);\n" +
                    "  vShade = 0.55 + 0.45 * d;\n" +
                    "}";

    private static final String FS =
            "precision mediump float;\n" +
                    "uniform vec3 uColor;\n" +
                    "varying float vShade;\n" +
                    "void main() {\n" +
                    "  gl_FragColor = vec4(uColor * vShade, 1.0);\n" +
                    "}";

    private final World world;
    private int program;
    private int aPos;
    private int aNormal;
    private int uMvp;
    private int uColor;
    private FloatBuffer cube;

    private final float[] proj = new float[16];
    private final float[] view = new float[16];
    private final float[] model = new float[16];
    private final float[] temp = new float[16];
    private final float[] mvp = new float[16];
    private final float[] viewProj = new float[16];
    private final Object lock = new Object();

    private int width = 1;
    private int height = 1;
    private long lastTime;
    private double walkPhase;
    private float vy;

    private float skyR = 0.10f;
    private float skyG = 0.13f;
    private float skyB = 0.18f;

    public WorldRenderer(World world) {
        this.world = world;
    }

    public void setSky(int color) {
        skyR = ((color >> 16) & 0xFF) / 255f;
        skyG = ((color >> 8) & 0xFF) / 255f;
        skyB = (color & 0xFF) / 255f;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glClearColor(skyR, skyG, skyB, 1f);
        buildCube();
        buildProgram();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int w, int h) {
        width = Math.max(1, w);
        height = Math.max(1, h);
        GLES20.glViewport(0, 0, width, height);
        Matrix.perspectiveM(proj, 0, 62f, (float) width / (float) height, 0.25f, 1400f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        update();

        GLES20.glClearColor(skyR, skyG, skyB, 1f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(program);

        float cy = world.camYaw;
        float pitch = world.camPitch;
        float dist = 17f;
        float cosP = (float) Math.cos(pitch);
        float ex = world.x + (float) Math.sin(cy) * dist * cosP;
        float ez = world.z + (float) Math.cos(cy) * dist * cosP;
        float ey = world.y + 5.2f + (float) Math.sin(pitch) * dist;

        Matrix.setLookAtM(view, 0, ex, ey, ez, world.x, world.y + 2.6f, world.z, 0f, 1f, 0f);
        synchronized (lock) {
            Matrix.multiplyMM(viewProj, 0, proj, 0, view, 0);
        }

        drawGround();
        drawParts();
        drawPlayers();
    }

    private void update() {
        long now = System.nanoTime();
        if (lastTime == 0) lastTime = now;
        float dt = (now - lastTime) / 1_000_000_000f;
        lastTime = now;
        if (dt > 0.08f) dt = 0.08f;
        if (dt <= 0) return;

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

        float nx = world.x + vx * dt;
        float nz = world.z + vz * dt;
        world.x = Math.max(-250f, Math.min(250f, nx));
        world.z = Math.max(-250f, Math.min(250f, nz));

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

    private void drawGround() {
        float tile = 64f;
        for (int ix = -4; ix < 4; ix++) {
            for (int iz = -4; iz < 4; iz++) {
                boolean even = ((ix + iz) & 1) == 0;
                int color = even ? 0xFFD9DEE7 : 0xFFB6BEC9;
                drawBox(ix * tile + tile / 2f, -0.5f, iz * tile + tile / 2f, tile, 1f, tile, 0f, color);
            }
        }

        drawBox(0, 0.02f, 0, 15f, 0.08f, 15f, 0f, 0xFF00A2FF);
        drawBox(0, 0.04f, 0, 11f, 0.08f, 11f, 0f, 0xFF1B1F27);

        int wall = 0xFF3B4250;
        drawBox(0, 1f, 255f, 512f, 2f, 3f, 0f, wall);
        drawBox(0, 1f, -255f, 512f, 2f, 3f, 0f, wall);
        drawBox(255f, 1f, 0, 3f, 2f, 512f, 0f, wall);
        drawBox(-255f, 1f, 0, 3f, 2f, 512f, 0f, wall);

        drawBox(18, 2, 14, 6, 4, 6, 0f, 0xFF7C5CFF);
        drawBox(-22, 3, 18, 8, 6, 8, 0f, 0xFF00C896);
        drawBox(-16, 1.5f, -26, 10, 3, 6, 0f, 0xFFE0A72E);
        drawBox(26, 4, -20, 7, 8, 7, 0f, 0xFFC64B8C);
        drawBox(0, 1, -40, 26, 2, 6, 0f, 0xFF30343B);
    }

    private void drawParts() {
        List<PlacePart> parts = world.parts;
        int n = Math.min(parts.size(), 1600);
        for (int i = 0; i < n; i++) {
            PlacePart p = parts.get(i);
            drawBox(p.x, p.y, p.z, p.sx, p.sy, p.sz, p.yaw, p.color);
        }
    }

    private void drawPlayers() {
        drawAvatar(world.x, world.y, world.z, world.yaw,
                Catalog.shirtColor(world.shirt), Catalog.pantsColor(world.pants), walkPhase);

        RevivalSession s = RevivalSession.current;
        if (s == null) return;
        for (RemotePlayer p : s.snapshot()) {
            if (p.id == s.localId) continue;
            drawAvatar(p.x, p.y, p.z, p.yaw, Catalog.shirtColor(p.shirt),
                    Catalog.pantsColor(p.pants), 0);
        }
    }

    private void drawAvatar(float x, float y, float z, float yawDeg, int shirt, int pants, double phase) {
        int skin = 0xFFF2C24B;
        float bob = (float) Math.abs(Math.sin(phase)) * 0.10f;
        float swing = (float) Math.sin(phase) * 0.28f;

        // ноги
        local(x, y, z, yawDeg, -0.5f, 1f, swing, 1f, 2f, 1f, pants);
        local(x, y, z, yawDeg, 0.5f, 1f, -swing, 1f, 2f, 1f, pants);
        // торс
        local(x, y, z, yawDeg, 0f, 2f + bob, 0f, 2f, 2f, 1f, shirt);
        // руки
        local(x, y, z, yawDeg, -1.5f, 2f + bob, -swing * 0.6f, 1f, 2f, 1f, shirt);
        local(x, y, z, yawDeg, 1.5f, 2f + bob, swing * 0.6f, 1f, 2f, 1f, shirt);
        // голова
        local(x, y, z, yawDeg, 0f, 3.65f + bob, 0f, 1.3f, 1.3f, 1.3f, skin);

        drawBox(x, 0.02f, z, 2.8f, 0.04f, 2.8f, 0f, 0x60000000);
    }

    private void local(float x, float y, float z, float yawDeg,
                       float ox, float oy, float oz,
                       float sx, float sy, float sz, int color) {
        double rad = Math.toRadians(yawDeg);
        float cs = (float) Math.cos(rad);
        float sn = (float) Math.sin(rad);
        float wx = x + ox * cs + oz * sn;
        float wz = z - ox * sn + oz * cs;
        drawBox(wx, y + oy, wz, sx, sy, sz, yawDeg, color);
    }

    private void drawBox(float x, float y, float z, float sx, float sy, float sz, float yaw, int color) {
        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, x, y, z);
        if (yaw != 0) Matrix.rotateM(model, 0, yaw, 0, 1, 0);
        Matrix.scaleM(model, 0, sx, sy, sz);

        Matrix.multiplyMM(temp, 0, view, 0, model, 0);
        Matrix.multiplyMM(mvp, 0, proj, 0, temp, 0);
        GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0);
        GLES20.glUniform3f(uColor, ((color >> 16) & 0xFF) / 255f,
                ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);
    }

    public float[] project(float x, float y, float z) {
        synchronized (lock) {
            float[] in = new float[]{x, y, z, 1f};
            float[] out = new float[4];
            Matrix.multiplyMV(out, 0, viewProj, 0, in, 0);
            if (out[3] <= 0.0001f) return null;
            float nx = out[0] / out[3];
            float ny = out[1] / out[3];
            return new float[]{(nx * 0.5f + 0.5f) * width, (1f - (ny * 0.5f + 0.5f)) * height};
        }
    }

    private void buildProgram() {
        int vs = compile(GLES20.GL_VERTEX_SHADER, VS);
        int fs = compile(GLES20.GL_FRAGMENT_SHADER, FS);
        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vs);
        GLES20.glAttachShader(program, fs);
        GLES20.glLinkProgram(program);
        aPos = GLES20.glGetAttribLocation(program, "aPos");
        aNormal = GLES20.glGetAttribLocation(program, "aNormal");
        uMvp = GLES20.glGetUniformLocation(program, "uMvp");
        uColor = GLES20.glGetUniformLocation(program, "uColor");

        cube.position(0);
        GLES20.glEnableVertexAttribArray(aPos);
        GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 24, cube);
        cube.position(3);
        GLES20.glEnableVertexAttribArray(aNormal);
        GLES20.glVertexAttribPointer(aNormal, 3, GLES20.GL_FLOAT, false, 24, cube);
        cube.position(0);
    }

    private int compile(int type, String src) {
        int sh = GLES20.glCreateShader(type);
        GLES20.glShaderSource(sh, src);
        GLES20.glCompileShader(sh);
        return sh;
    }

    private void buildCube() {
        float[] v = {
                0.5f, -0.5f, -0.5f, 1, 0, 0,
                0.5f, -0.5f, 0.5f, 1, 0, 0,
                0.5f, 0.5f, 0.5f, 1, 0, 0,
                0.5f, -0.5f, -0.5f, 1, 0, 0,
                0.5f, 0.5f, 0.5f, 1, 0, 0,
                0.5f, 0.5f, -0.5f, 1, 0, 0,

                -0.5f, -0.5f, 0.5f, -1, 0, 0,
                -0.5f, -0.5f, -0.5f, -1, 0, 0,
                -0.5f, 0.5f, -0.5f, -1, 0, 0,
                -0.5f, -0.5f, 0.5f, -1, 0, 0,
                -0.5f, 0.5f, -0.5f, -1, 0, 0,
                -0.5f, 0.5f, 0.5f, -1, 0, 0,

                -0.5f, 0.5f, 0.5f, 0, 1, 0,
                0.5f, 0.5f, 0.5f, 0, 1, 0,
                0.5f, 0.5f, -0.5f, 0, 1, 0,
                -0.5f, 0.5f, 0.5f, 0, 1, 0,
                0.5f, 0.5f, -0.5f, 0, 1, 0,
                -0.5f, 0.5f, -0.5f, 0, 1, 0,

                -0.5f, -0.5f, -0.5f, 0, -1, 0,
                0.5f, -0.5f, -0.5f, 0, -1, 0,
                0.5f, -0.5f, 0.5f, 0, -1, 0,
                -0.5f, -0.5f, -0.5f, 0, -1, 0,
                0.5f, -0.5f, 0.5f, 0, -1, 0,
                -0.5f, -0.5f, 0.5f, 0, -1, 0,

                -0.5f, -0.5f, 0.5f, 0, 0, 1,
                0.5f, -0.5f, 0.5f, 0, 0, 1,
                0.5f, 0.5f, 0.5f, 0, 0, 1,
                -0.5f, -0.5f, 0.5f, 0, 0, 1,
                0.5f, 0.5f, 0.5f, 0, 0, 1,
                -0.5f, 0.5f, 0.5f, 0, 0, 1,

                0.5f, -0.5f, -0.5f, 0, 0, -1,
                -0.5f, -0.5f, -0.5f, 0, 0, -1,
                -0.5f, 0.5f, -0.5f, 0, 0, -1,
                0.5f, -0.5f, -0.5f, 0, 0, -1,
                -0.5f, 0.5f, -0.5f, 0, 0, -1,
                0.5f, 0.5f, -0.5f, 0, 0, -1
        };
        ByteBuffer bb = ByteBuffer.allocateDirect(v.length * 4);
        bb.order(ByteOrder.nativeOrder());
        cube = bb.asFloatBuffer();
        cube.put(v);
        cube.position(0);
    }
}
