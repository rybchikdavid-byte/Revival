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
    public static int pants(Context c) { return sp(c).getInt("pants", 0); }
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
}
