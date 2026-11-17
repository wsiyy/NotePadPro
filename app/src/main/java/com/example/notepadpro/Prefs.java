package com.example.notepadpro;

import android.content.Context;
import android.content.SharedPreferences;

/** 简单偏好设置封装（目前只有深色模式）。 */
public final class Prefs {

    private static final String FILE = "notepad_prefs";
    private static final String KEY_DARK = "dark_mode";

    private Prefs() {
    }

    public static boolean isDark(Context context) {
        return prefs(context).getBoolean(KEY_DARK, false);
    }

    public static void setDark(Context context, boolean dark) {
        prefs(context).edit().putBoolean(KEY_DARK, dark).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }
}
