package com.example.notepadpro;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * 应用入口：在进程启动时恢复用户选择的深色模式，
 * 避免每次重建界面时闪烁。
 */
public class NotePadApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        boolean dark = Prefs.isDark(this);
        AppCompatDelegate.setDefaultNightMode(
                dark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }
}
