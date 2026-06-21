package com.dpis.module;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

final class RootAppProcessLauncher {
    private final Context context;

    RootAppProcessLauncher(Context context) {
        this.context = context;
    }

    ShellResult forceStop(String packageName) {
        if (!isSafePackageName(packageName)) {
            return new ShellResult(-1, "root stop unavailable");
        }
        return runSuCommand("am force-stop " + packageName);
    }

    ShellResult start(String packageName) {
        if (!isSafePackageName(packageName)) {
            return new ShellResult(-1, "root start unavailable");
        }
        ComponentName launchComponent = resolveLaunchComponent(packageName);
        if (launchComponent == null) {
            return new ShellResult(-1, "launcher activity not found");
        }
        return runSuCommand("am start --user current"
                + " -a android.intent.action.MAIN"
                + " -c android.intent.category.LAUNCHER"
                + " -n " + shellQuote(launchComponent.flattenToShortString()));
    }

    ShellResult restart(String packageName) {
        ShellResult stopResult = forceStop(packageName);
        if (stopResult.code != 0) {
            return stopResult;
        }
        return start(packageName);
    }

    private ComponentName resolveLaunchComponent(String packageName) {
        Intent launchIntent = context.getPackageManager()
                .getLaunchIntentForPackage(packageName);
        return launchIntent != null ? launchIntent.getComponent() : null;
    }

    private static boolean isSafePackageName(String packageName) {
        return packageName != null && packageName.matches("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+");
    }

    static String shellQuoteForTest(String value) {
        return shellQuote(value);
    }

    private static String shellQuote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }

    private ShellResult runSuCommand(String command) {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(new String[] { "su", "-c", command });
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
                    BufferedReader errReader = new BufferedReader(
                            new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    appendLine(output, line);
                }
                while ((line = errReader.readLine()) != null) {
                    appendLine(output, line);
                }
            }
            return new ShellResult(process.waitFor(), output.toString());
        } catch (IOException exception) {
            return new ShellResult(-1, exceptionMessage(exception));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new ShellResult(-1, exceptionMessage(exception));
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private static void appendLine(StringBuilder output, String line) {
        if (output.length() > 0) {
            output.append('\n');
        }
        output.append(line);
    }

    private static String exceptionMessage(Exception exception) {
        return exception.getMessage() != null
                ? exception.getMessage()
                : exception.getClass().getSimpleName();
    }

    static final class ShellResult {
        final int code;
        final String output;

        ShellResult(int code, String output) {
            this.code = code;
            this.output = output != null ? output : "";
        }
    }
}
