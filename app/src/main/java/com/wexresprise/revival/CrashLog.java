package com.wexresprise.revival;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Ловит необработанные ошибки, кладёт текст в Downloads/Revival-crash.txt
 * и открывает экран с логом в отдельном процессе (чтобы он пережил падение).
 */
public final class CrashLog {

    public static volatile boolean inCrashProcess;
    private static volatile long lastUiReport;

    private CrashLog() {
    }

    public static void install(Context c) {
        if (inCrashProcess) return;
        final Context app = c.getApplicationContext();
        final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            try {
                save(app, thread, error);
            } catch (Throwable ignored) {
            }
            try {
                if (!inCrashProcess) {
                    Intent i = new Intent(app, CrashActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    app.startActivity(i);
                }
            } catch (Throwable ignored) {
            }
            if (previous != null) {
                previous.uncaughtException(thread, error);
            } else {
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(10);
            }
        });
    }

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("revival_diag", Context.MODE_PRIVATE);
    }

    public static File latestFile(Context c) {
        return new File(c.getFilesDir(), "crash-latest.txt");
    }

    public static String latest(Context c) {
        try {
            if (c == null) return "";
            File f = latestFile(c.getApplicationContext());
            if (f.exists()) return Diag.read(f);
        } catch (Throwable ignored) {
        }
        return "";
    }

    public static boolean hasUnseen(Context c) {
        return c != null && sp(c.getApplicationContext()).getBoolean("crash_unseen", false);
    }

    public static void markSeen(Context c) {
        if (c == null) return;
        sp(c.getApplicationContext()).edit().putBoolean("crash_unseen", false).apply();
    }

    public static void save(Context c, Thread thread, Throwable error) {
        if (c == null) return;
        Context app = c.getApplicationContext();
        String text = build(app, thread, error);
        try {
            Diag.write(latestFile(app), text);
        } catch (Throwable ignored) {
        }
        try {
            File ext = app.getExternalFilesDir("crash");
            if (ext != null) {
                ext.mkdirs();
                Diag.write(new File(ext, "latest.txt"), text);
            }
        } catch (Throwable ignored) {
        }
        try {
            writeDownload(app, "Revival-crash.txt", text);
        } catch (Throwable ignored) {
        }
        try {
            sp(app).edit()
                    .putBoolean("crash_unseen", true)
                    .putLong("crash_at", System.currentTimeMillis())
                    .apply();
        } catch (Throwable ignored) {
        }
    }

    public static void saveBootFailure(Context c, String diag) {
        if (c == null) return;
        Context app = c.getApplicationContext();
        String text = "Revival: прошлый запуск завершился аварийно (процесс умер, не успев закрыться).\n"
                + "Время: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()) + "\n"
                + "Устройство: " + Build.MANUFACTURER + " " + Build.MODEL
                + ", Android " + Build.VERSION.RELEASE + "\n"
                + "----------------------------------------\n"
                + diag + "\n";
        try {
            Diag.write(latestFile(app), text);
        } catch (Throwable ignored) {
        }
        try {
            writeDownload(app, "Revival-previous-run.txt", text);
        } catch (Throwable ignored) {
        }
        try {
            sp(app).edit()
                    .putBoolean("crash_unseen", true)
                    .putLong("crash_at", System.currentTimeMillis())
                    .apply();
        } catch (Throwable ignored) {
        }
    }

    private static String build(Context c, Thread thread, Throwable error) {
        StringBuilder sb = new StringBuilder();
        sb.append("Revival crash\n");
        sb.append("Время: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                .format(new Date())).append('\n');
        sb.append("Версия: ").append(BuildConfig.VERSION_NAME)
                .append(" (").append(BuildConfig.VERSION_CODE).append(")\n");
        sb.append("Устройство: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL)
                .append(", Android ").append(Build.VERSION.RELEASE)
                .append(" (SDK ").append(Build.VERSION.SDK_INT).append(")\n");
        sb.append("Поток: ").append(thread == null ? "?" : thread.getName()).append('\n');
        sb.append("----------------------------------------\n");
        sb.append(android.util.Log.getStackTraceString(error)).append('\n');
        sb.append("----------------------------------------\n");
        sb.append("Дневник:\n").append(Diag.tail(c, 300)).append('\n');
        return sb.toString();
    }

    /** Ошибка, пойматая в обработчике нажатия или отрисовке: приложение продолжает работать. */
    public static void reportUi(final Context c, final Throwable error) {
        if (c == null || error == null) return;
        try {
            save(c, Thread.currentThread(), error);
        } catch (Throwable ignored) {
        }
        final long now = SystemClock.elapsedRealtime();
        if (now - lastUiReport < 4000) return;
        lastUiReport = now;
        if (!(c instanceof Activity)) {
            try {
                Toast.makeText(c, "Ошибка Revival: " + shortText(error), Toast.LENGTH_LONG).show();
            } catch (Throwable ignored) {
            }
            return;
        }
        final Activity activity = (Activity) c;
        activity.runOnUiThread(() -> {
            try {
                if (activity.isFinishing() || activity.isDestroyed()) return;
                new AlertDialog.Builder(activity)
                        .setTitle("Ошибка Revival")
                        .setMessage(shortText(error)
                                + "\n\nЛог сохранён в Downloads/Revival-crash.txt")
                        .setPositiveButton("Показать лог", (d, w) -> {
                            try {
                                activity.startActivity(new Intent(activity, CrashActivity.class));
                            } catch (Throwable ignored) {
                            }
                        })
                        .setNegativeButton("Закрыть", null)
                        .show();
            } catch (Throwable t) {
                try {
                    Toast.makeText(activity, "Ошибка: " + shortText(error), Toast.LENGTH_LONG).show();
                } catch (Throwable ignored) {
                }
            }
        });
    }

    private static String shortText(Throwable error) {
        String msg = error.getMessage();
        String s = error.getClass().getSimpleName() + (msg == null ? "" : ": " + msg);
        if (s.length() > 500) s = s.substring(0, 500) + "…";
        return s;
    }

    /** Сохраняет текст в Downloads через MediaStore. Возвращает "Download/имя" или null. */
    public static String writeDownload(Context c, String name, String text) {
        Context app = c.getApplicationContext();
        if (Build.VERSION.SDK_INT >= 29) {
            ContentResolver resolver = app.getContentResolver();
            Uri collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
            try {
                resolver.delete(collection, MediaStore.Downloads.DISPLAY_NAME + "=?",
                        new String[]{name});
            } catch (Throwable ignored) {
            }
            Uri uri = null;
            try {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, name);
                values.put(MediaStore.Downloads.MIME_TYPE, "text/plain");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/");
                values.put(MediaStore.Downloads.IS_PENDING, 1);
                uri = resolver.insert(collection, values);
                if (uri == null) return null;
                try (OutputStream out = resolver.openOutputStream(uri)) {
                    if (out == null) return null;
                    out.write(text.getBytes("UTF-8"));
                }
                ContentValues done = new ContentValues();
                done.put(MediaStore.Downloads.IS_PENDING, 0);
                resolver.update(uri, done, null, null);
                return Environment.DIRECTORY_DOWNLOADS + "/" + name;
            } catch (Throwable t) {
                if (uri != null) {
                    try {
                        resolver.delete(uri, null, null);
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
        try {
            File dir = app.getExternalFilesDir(null);
            if (dir == null) return null;
            File f = new File(dir, name);
            try (FileOutputStream out = new FileOutputStream(f, false)) {
                out.write(text.getBytes("UTF-8"));
            }
            return f.getAbsolutePath();
        } catch (Throwable t) {
            return null;
        }
    }
}
