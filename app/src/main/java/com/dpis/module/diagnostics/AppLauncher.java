package com.dpis.module.diagnostics;

import android.content.Context;

import com.dpis.module.root.RootAppProcessLauncher;

public final class AppLauncher {
    private final RootAppProcessLauncher rootLauncher;

    public AppLauncher(Context context) {
        rootLauncher = new RootAppProcessLauncher(context);
    }

    public boolean restartForDiagnostic(String packageName) {
        return rootLauncher.restart(packageName).code() == 0;
    }
}
