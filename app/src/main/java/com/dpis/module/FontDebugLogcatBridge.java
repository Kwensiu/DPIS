package com.dpis.module;

import android.content.Context;
import android.os.Bundle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

final class FontDebugLogcatBridge {
    private static final int MAX_LINES = 300;
    private static final int TOP_LIMIT = 20;
    private static final String[] MARKERS = {
            "DPIS_FONT TextPaint.setTextSize override",
            "DPIS_FONT Paint.setTextSize override",
            "DPIS_FONT ForceTextSize override",
            "DPIS_FONT TextView span override",
            "DPIS_FONT SystemServer config fontScale"
    };

    private FontDebugLogcatBridge() {
    }

    static boolean importRecent(Context context) {
        if (context == null) {
            return false;
        }
        Map<String, Integer> counts = new LinkedHashMap<>();
        Process process = null;
        try {
            process = new ProcessBuilder("logcat", "-d", "-t", String.valueOf(MAX_LINES),
                    "-s", "DPIS:I", "*:S").redirectErrorStream(true).start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String key = resolveKey(line);
                    if (key != null) {
                        counts.put(key, counts.getOrDefault(key, 0) + 1);
                    }
                }
            }
            process.waitFor();
        } catch (IOException ignored) {
            return false;
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        if (counts.isEmpty()) {
            return true;
        }
        String formatted = formatCounts(counts);
        Bundle extras = new Bundle();
        extras.putString(FontDebugStatsStore.EXTRA_CHAIN_5S, formatted);
        extras.putString(FontDebugStatsStore.EXTRA_CHAIN_30S, formatted);
        extras.putString(FontDebugStatsStore.EXTRA_CHAIN_ALL, formatted);
        extras.putInt(FontDebugStatsStore.EXTRA_EVENT_TOTAL,
                counts.values().stream().mapToInt(Integer::intValue).sum());
        extras.putLong(FontDebugStatsStore.EXTRA_UPDATED_AT, System.currentTimeMillis());
        FontDebugStatsUpdateWriter.applyExtras(context, extras);
        return true;
    }

    private static String resolveKey(String line) {
        if (line == null) {
            return null;
        }
        for (String marker : MARKERS) {
            int index = line.indexOf(marker);
            if (index >= 0) {
                return marker.replace("DPIS_FONT ", "").replace(" override", "");
            }
        }
        return null;
    }

    private static String formatCounts(Map<String, Integer> counts) {
        StringBuilder builder = new StringBuilder();
        int emitted = 0;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (emitted > 0) {
                builder.append('\n');
            }
            builder.append(entry.getValue()).append(' ').append(entry.getKey());
            emitted++;
            if (emitted >= TOP_LIMIT) {
                break;
            }
        }
        return builder.toString();
    }
}
