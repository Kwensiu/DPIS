package com.dpis.module;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FontDebugOverlayService extends Service {
    private static final long REFRESH_INTERVAL_MS = 500L;
    private static final Pattern UNIT_LINE_PATTERN =
            Pattern.compile("^\\s*(\\d+)\\s+text-size-unit-(\\d)\\b.*$");

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            renderOverlayText();
            handler.postDelayed(this, REFRESH_INTERVAL_MS);
        }
    };

    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;
    private TextView overlayTextView;
    private View overlayRoot;
    private DpiConfigStore store;
    private GestureDetector gestureDetector;

    @Override
    public void onCreate() {
        super.onCreate();
        store = ConfigStoreFactory.createForModuleApp(this);
        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (windowManager == null) {
            stopSelf();
            return;
        }
        createOverlayView();
        handler.post(refreshRunnable);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (overlayRoot == null && windowManager != null) {
            createOverlayView();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(refreshRunnable);
        if (windowManager != null && overlayRoot != null) {
            try {
                windowManager.removeView(overlayRoot);
            } catch (Throwable ignored) {
            }
        }
        overlayRoot = null;
        overlayTextView = null;
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createOverlayView() {
        overlayTextView = new TextView(this);
        overlayTextView.setTextSize(11f);
        overlayTextView.setTextColor(Color.WHITE);
        overlayTextView.setPadding(dp(10), dp(8), dp(10), dp(8));
        overlayTextView.setMaxWidth(dp(280));
        overlayTextView.setTypeface(android.graphics.Typeface.MONOSPACE);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xCC111111);
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1), 0x66FFFFFF);
        overlayTextView.setBackground(bg);

        overlayRoot = overlayTextView;
        gestureDetector = new GestureDetector(this, new OverlayGestureListener());
        overlayRoot.setOnClickListener(v -> cycleWindowMode());
        overlayRoot.setOnLongClickListener(v -> {
            cycleGroupMode();
            return true;
        });
        overlayRoot.setOnTouchListener(new DragTouchListener());

        layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );
        layoutParams.gravity = Gravity.TOP | Gravity.END;
        layoutParams.x = dp(12);
        layoutParams.y = dp(120);

        windowManager.addView(overlayRoot, layoutParams);
        renderOverlayText();
    }

    private void cycleWindowMode() {
        if (store == null) {
            return;
        }
        int current = store.getFontDebugSelectedWindow();
        int next;
        if (current == FontDebugStatsStore.WINDOW_5S) {
            next = FontDebugStatsStore.WINDOW_30S;
        } else if (current == FontDebugStatsStore.WINDOW_30S) {
            next = FontDebugStatsStore.WINDOW_ALL;
        } else {
            next = FontDebugStatsStore.WINDOW_5S;
        }
        store.setFontDebugSelectedWindow(next);
        renderOverlayText();
    }

    private void cycleGroupMode() {
        if (store == null) {
            return;
        }
        int current = store.getFontDebugSelectedMode();
        int next = current == FontDebugStatsStore.MODE_CHAIN
                ? FontDebugStatsStore.MODE_CHAIN_VIEW
                : FontDebugStatsStore.MODE_CHAIN;
        store.setFontDebugSelectedMode(next);
        renderOverlayText();
    }

    private void renderOverlayText() {
        if (overlayTextView == null) {
            return;
        }
        android.content.SharedPreferences preferences = FontDebugStatsStore.getPreferences(this);
        int eventTotal = preferences.getInt(FontDebugStatsStore.KEY_EVENT_TOTAL, 0);
        long updatedAt = preferences.getLong(FontDebugStatsStore.KEY_UPDATED_AT, 0L);
        int mode = store != null ? store.getFontDebugSelectedMode() : FontDebugStatsStore.MODE_CHAIN;
        int window = store != null ? store.getFontDebugSelectedWindow() : FontDebugStatsStore.WINDOW_ALL;

        String statsKey = resolveStatsKey(mode, window);
        String statsText = preferences.getString(statsKey, "暂无数据");
        String unitBreakdown = buildUnitBreakdownFromStats(statsText);
        if ("unit: 0=0 1=0 2=0".equals(unitBreakdown)) {
            unitBreakdown = preferences.getString(FontDebugStatsStore.KEY_UNIT_BREAKDOWN_5S,
                    "unit: 0=0 1=0 2=0");
        }
        int topLimit = store != null
                ? Math.max(3, store.getDebugInt(FontDebugStatsStore.KEY_FONT_DEBUG_OVERLAY_TOP_LIMIT, 3))
                : 3;
        String[] lines = (statsText == null ? "暂无数据" : statsText).split("\n");
        StringBuilder top = new StringBuilder();
        int limit = Math.min(topLimit, lines.length);
        for (int i = 0; i < limit; i++) {
            top.append(lines[i]);
            if (i < limit - 1) {
                top.append('\n');
            }
        }

        String modeText = mode == FontDebugStatsStore.MODE_CHAIN ? "链路" : "链路+视图";
        String windowText = switch (window) {
            case FontDebugStatsStore.WINDOW_5S -> "5秒";
            case FontDebugStatsStore.WINDOW_30S -> "30秒";
            default -> "累计";
        };
        String body = String.format(Locale.US,
                "字体统计 %s %s Top%d\n总事件:%d\n%s\n%s\n\n点按:切窗口  双击:切Top  长按:切分组",
                windowText,
                modeText,
                topLimit,
                eventTotal,
                unitBreakdown,
                top);
        if (updatedAt <= 0L) {
            body = "字体统计 初始化中\n点按:切窗口  长按:切分组";
        }
        overlayTextView.setText(body);
    }

    private static String buildUnitBreakdownFromStats(String statsText) {
        if (statsText == null || statsText.isEmpty()) {
            return "unit: 0=0 1=0 2=0";
        }
        int unit0 = 0;
        int unit1 = 0;
        int unit2 = 0;
        String[] lines = statsText.split("\n");
        for (String line : lines) {
            if (line == null || line.isEmpty()) {
                continue;
            }
            Matcher matcher = UNIT_LINE_PATTERN.matcher(line);
            if (!matcher.matches()) {
                continue;
            }
            int count;
            int unit;
            try {
                count = Integer.parseInt(matcher.group(1));
                unit = Integer.parseInt(matcher.group(2));
            } catch (NumberFormatException ignored) {
                continue;
            }
            if (count <= 0) {
                continue;
            }
            if (unit == 0) {
                unit0 += count;
            } else if (unit == 1) {
                unit1 += count;
            } else if (unit == 2) {
                unit2 += count;
            }
        }
        return "unit: 0=" + unit0 + " 1=" + unit1 + " 2=" + unit2;
    }

    private static String resolveStatsKey(int mode, int window) {
        if (mode == FontDebugStatsStore.MODE_CHAIN_VIEW) {
            if (window == FontDebugStatsStore.WINDOW_5S) {
                return FontDebugStatsStore.KEY_CHAIN_VIEW_5S;
            }
            if (window == FontDebugStatsStore.WINDOW_30S) {
                return FontDebugStatsStore.KEY_CHAIN_VIEW_30S;
            }
            return FontDebugStatsStore.KEY_CHAIN_VIEW_ALL;
        }
        if (window == FontDebugStatsStore.WINDOW_5S) {
            return FontDebugStatsStore.KEY_CHAIN_5S;
        }
        if (window == FontDebugStatsStore.WINDOW_30S) {
            return FontDebugStatsStore.KEY_CHAIN_30S;
        }
        return FontDebugStatsStore.KEY_CHAIN_ALL;
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    private final class DragTouchListener implements View.OnTouchListener {
        private int startX;
        private int startY;
        private float touchStartX;
        private float touchStartY;
        private boolean dragging;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (gestureDetector != null) {
                gestureDetector.onTouchEvent(event);
            }
            if (layoutParams == null || windowManager == null) {
                return false;
            }
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    startX = layoutParams.x;
                    startY = layoutParams.y;
                    touchStartX = event.getRawX();
                    touchStartY = event.getRawY();
                    dragging = false;
                    return false;
                case MotionEvent.ACTION_MOVE:
                    int dx = Math.round(event.getRawX() - touchStartX);
                    int dy = Math.round(event.getRawY() - touchStartY);
                    if (Math.abs(dx) > dp(4) || Math.abs(dy) > dp(4)) {
                        dragging = true;
                    }
                    if (dragging) {
                        layoutParams.x = startX - dx;
                        layoutParams.y = startY + dy;
                        windowManager.updateViewLayout(overlayRoot, layoutParams);
                        return true;
                    }
                    return false;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    return dragging;
                default:
                    return false;
            }
        }
    }

    private final class OverlayGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            cycleTopLimit();
            return true;
        }
    }

    private void cycleTopLimit() {
        if (store == null) {
            return;
        }
        int current = Math.max(3, store.getDebugInt(
                FontDebugStatsStore.KEY_FONT_DEBUG_OVERLAY_TOP_LIMIT, 3));
        int next = current >= 10 ? 3 : 10;
        store.setDebugInt(FontDebugStatsStore.KEY_FONT_DEBUG_OVERLAY_TOP_LIMIT, next);
        renderOverlayText();
    }
}
