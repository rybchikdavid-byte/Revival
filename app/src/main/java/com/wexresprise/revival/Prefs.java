package com.wexresprise.revival;

import android.content.Context;
import android.content.SharedPreferences;

public final class Prefs {

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("revival", Context.MODE_PRIVATE);
    }

    public static int year(Context c) {
        return sp(c).getInt("year", 2016);
    }

    public static void setYear(Context c, int y) {
        sp(c).edit().putInt("year", y).apply();
    }

    public static String nick(Context c) {
        String n = sp(c).getString("nick", "");
        if (n == null || n.trim().isEmpty()) n = "Player";
        return n;
    }

    public static void setNick(Context c, String n) {
        sp(c).edit().putString("nick", n).apply();
    }

    public static int shirt(Context c) { return sp(c).getInt("shirt", 0); }
    public static int pants(Context c) { return sp(c).getInt("pants", 2); }
    public static int face(Context c) { return sp(c).getInt("face", 0); }
    public static int hair(Context c) { return sp(c).getInt("hair", 0); }

    public static void setShirt(Context c, int v) { sp(c).edit().putInt("shirt", v).apply(); }
    public static void setPants(Context c, int v) { sp(c).edit().putInt("pants", v).apply(); }
    public static void setFace(Context c, int v) { sp(c).edit().putInt("face", v).apply(); }
    public static void setHair(Context c, int v) { sp(c).edit().putInt("hair", v).apply(); }

    public static String customShirt(Context c) { return sp(c).getString("custom_shirt", null); }
    public static void setCustomShirt(Context c, String path) { sp(c).edit().putString("custom_shirt", path).apply(); }

    public static Avatar avatar(Context c) {
        Avatar a = new Avatar();
        a.shirt = shirt(c);
        a.pants = pants(c);
        a.face = face(c);
        a.hair = hair(c);
        return a;
    }

    // ---- настройки клиента

    public static boolean shiftLock(Context c) { return sp(c).getBoolean("shift_lock", false); }
    public static void setShiftLock(Context c, boolean v) { sp(c).edit().putBoolean("shift_lock", v).apply(); }

    public static int cameraMode(Context c) { return sp(c).getInt("camera_mode", 0); }
    public static void setCameraMode(Context c, int v) { sp(c).edit().putInt("camera_mode", v).apply(); }

    public static int movementMode(Context c) { return sp(c).getInt("movement_mode", 0); }
    public static void setMovementMode(Context c, int v) { sp(c).edit().putInt("movement_mode", v).apply(); }

    public static int sensitivity(Context c) { return sp(c).getInt("sensitivity", 5); }
    public static void setSensitivity(Context c, int v) { sp(c).edit().putInt("sensitivity", v).apply(); }

    public static int volume(Context c) { return sp(c).getInt("volume", 10); }
    public static void setVolume(Context c, int v) { sp(c).edit().putInt("volume", v).apply(); }

    public static boolean fullscreen(Context c) { return sp(c).getBoolean("fullscreen", true); }
    public static void setFullscreen(Context c, boolean v) { sp(c).edit().putBoolean("fullscreen", v).apply(); }

    public static int graphicsMode(Context c) { return sp(c).getInt("graphics_mode", 0); }
    public static void setGraphicsMode(Context c, int v) { sp(c).edit().putInt("graphics_mode", v).apply(); }

    public static int graphicsQuality(Context c) { return sp(c).getInt("graphics_quality", 5); }
    public static void setGraphicsQuality(Context c, int v) { sp(c).edit().putInt("graphics_quality", v).apply(); }

    public static boolean perfStats(Context c) { return sp(c).getBoolean("perf_stats", false); }
    public static void setPerfStats(Context c, boolean v) { sp(c).edit().putBoolean("perf_stats", v).apply(); }

    public static boolean collisions(Context c) { return sp(c).getBoolean("collisions", false); }
    public static void setCollisions(Context c, boolean v) { sp(c).edit().putBoolean("collisions", v).apply(); }
}
