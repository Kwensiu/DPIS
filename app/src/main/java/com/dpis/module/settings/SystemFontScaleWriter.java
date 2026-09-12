package com.dpis.module.settings;

public final class SystemFontScaleWriter {
    public interface Host {
        boolean writePercent(int percent);

        void onWriteSucceeded(int percent);

        void onWriteFailed();
    }

    private SystemFontScaleWriter() {
    }

    public static void write(Host host, int percent) {
        if (host == null) {
            return;
        }
        try {
            if (!host.writePercent(percent)) {
                host.onWriteFailed();
                return;
            }
            host.onWriteSucceeded(percent);
        } catch (RuntimeException e) {
            host.onWriteFailed();
        }
    }
}
