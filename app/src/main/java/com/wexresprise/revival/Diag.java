package com.wexresprise.revival;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Deque;
import java.util.Locale;

/**
 * Короткий дневник запусков. Пишется в личные файлы приложения после каждой
 * строки, поэтому его видно даже если процесс умер в нативном коде.
 */
public final class Diag {

    private static final String TAG = "Revival";
    private static final Object LOCK = new Object();
    private static final Deque<String> RING = new ArrayDeque<>();
    private static final int RING_MAX = 400;
    private static final long FILE_MAX = 256 * 1024;

    private static final SimpleDateFormat TIME =
            new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);

    private Diag() {
    }

    public static String processName() {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                new FileInputStream("/proc/self/cmdline"), StandardCharsets.UTF_8))) {
            String s = r.readLine();
            if (s == null) return "";
            int z = s.indexOf('\0');
            if (z >= 0) s = s.substring(0, z);
            return s.trim();
        } catch (Throwable t) {
            return "";
        }
    }

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("revival_diag", Context.MODE_PRIVATE);
    }

    private static File file(Context c) {
        return new File(c.getFilesDir(), "diag.txt");
    }

    /** Вызывается один раз на процесс, до всего остального. */
    public static void startRun(Context c) {
        Context app = c.getApplicationContext();
        boolean started = sp(app).getBoolean("started", false);
        boolean complete = sp(app).getBoolean("complete", true);
        if (started && !complete) {
            // прошлый запуск не дошёл до паузы — значит упал, в том числе нативно
            int fails = sp(app).getInt("fails", 0) + 1;
            String old = read(file(app));
            if (old.isEmpty()) old = "(дневник пуст)";
            CrashLog.saveBootFailure(app, old);
            sp(app).edit().putBoolean("boot_notice", true)
                    .putInt("fails", fails)
                    .apply();
            // чтобы не попасть в бесконечный цикл падений, следующий запуск — без 3D
            if (!Prefs.safeMode(app)) {
                Prefs.setSafeMode(app, true);
            }
        }
        sp(app).edit().putBoolean("started", true)
                .putBoolean("complete", false)
                .apply();
        synchronized (LOCK) {
            RING.clear();
        }
        write(file(app), header(app));
        setRunning(app, true);
        line(app, "process " + processName());
    }

    private static String header(Context c) {
        return "Revival " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")\n"
                + "device: " + Build.MANUFACTURER + " " + Build.MODEL
                + " / Android " + Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")\n"
                + "started: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()) + "\n"
                + "----------------------------------------\n";
    }

    /** true — приложение на переднем плане; false — ушло в фон (не считаем падением). */
    public static void setRunning(Context c, boolean running) {
        android.content.SharedPreferences.Editor e = sp(c.getApplicationContext()).edit()
                .putBoolean("started", true)
                .putBoolean("complete", !running);
        if (!running) e.putInt("fails", 0);
        e.apply();
    }

    /** Сколько запусков подряд умерло, не дойдя до паузы. */
    public static int fails(Context c) {
        return sp(c.getApplicationContext()).getInt("fails", 0);
    }

    public static void resetFails(Context c) {
        if (c == null) return;
        sp(c.getApplicationContext()).edit().putInt("fails", 0).apply();
    }

    public static boolean bootNotice(Context c) {
        return sp(c.getApplicationContext()).getBoolean("boot_notice", false);
    }

    public static void clearBootNotice(Context c) {
        if (c == null) return;
        sp(c.getApplicationContext()).edit().putBoolean("boot_notice", false).apply();
    }

    public static void line(Context c, String message) {
        if (c == null) return;
        Context app = c.getApplicationContext();
        String s = TIME.format(new Date()) + "  " + message;
        synchronized (LOCK) {
            if (RING.size() >= RING_MAX) RING.removeFirst();
            RING.addLast(s);
            try {
                File f = file(app);
                if (f.length() > FILE_MAX) {
                    String tail = read(f);
                    if (tail.length() > 64 * 1024) {
                        tail = tail.substring(tail.length() - 64 * 1024);
                    }
                    write(f, "---- trimmed ----\n" + tail + "\n");
                }
                try (FileOutputStream out = new FileOutputStream(f, true)) {
                    out.write((s + "\n").getBytes(StandardCharsets.UTF_8));
                }
            } catch (Throwable ignored) {
            }
        }
        try {
            Log.i(TAG, message);
        } catch (Throwable ignored) {
        }
    }

    public static String tail(Context c, int lines) {
        if (c == null) return "";
        String all = read(file(c.getApplicationContext()));
        if (all.isEmpty()) {
            synchronized (LOCK) {
                StringBuilder sb = new StringBuilder();
                for (String s : RING) sb.append(s).append('\n');
                return sb.toString();
            }
        }
        String[] parts = all.split("\n", -1);
        int from = Math.max(0, parts.length - Math.max(1, lines));
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < parts.length; i++) {
            sb.append(parts[i]).append('\n');
        }
        return sb.toString();
    }

    public static String all(Context c) {
        if (c == null) return "";
        return read(file(c.getApplicationContext()));
    }

    static String read(File f) {
        try {
            if (!f.exists()) return "";
            try (FileInputStream in = new FileInputStream(f)) {
                byte[] buf = new byte[(int) Math.min(f.length(), FILE_MAX)];
                int n = in.read(buf);
                if (n <= 0) return "";
                return new String(buf, 0, n, StandardCharsets.UTF_8);
            }
        } catch (Throwable t) {
            return "";
        }
    }

    static void write(File f, String text) {
        try {
            File parent = f.getParentFile();
            if (parent != null) parent.mkdirs();
            try (FileOutputStream out = new FileOutputStream(f, false)) {
                out.write(text.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Throwable ignored) {
        }
    }
}
