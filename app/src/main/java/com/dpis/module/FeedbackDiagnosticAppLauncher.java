package com.dpis.module;

import android.content.Context;

final class FeedbackDiagnosticAppLauncher {
    private final RootAppProcessLauncher rootLauncher;

    FeedbackDiagnosticAppLauncher(Context context) {
        rootLauncher = new RootAppProcessLauncher(context);
    }

    boolean restartForDiagnostic(String packageName) {
        return rootLauncher.restart(packageName).code == 0;
    }
}
