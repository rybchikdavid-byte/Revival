package com.wexresprise.revival;

import android.app.Application;

public class RevivalApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        String process = Diag.processName();
        boolean crashProcess = process != null && process.endsWith(":crash");
        if (crashProcess) {
            // отдельный процесс экрана с логом: он не должен затирать дневник падения
            CrashLog.inCrashProcess = true;
            return;
        }
        CrashLog.install(this);
        Diag.startRun(this);
    }
}
