package com.dpis.module.fonts

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import com.dpis.module.ConfigStoreFactory
import com.dpis.module.DpisApplication
import com.dpis.module.DpisConfigStore
import com.dpis.module.R
import com.dpis.module.fonts.FontDebugDataDiagnostics.NoDataReason
import java.util.Locale
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.concurrent.Volatile
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class FontDebugOverlayService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var bridgeThread: HandlerThread? = null
    private var bridgeHandler: Handler? = null

    @Volatile
    private var lastBridgeImportAt: Long = 0

    @Volatile
    private var lastLogcatImportAt: Long = 0

    @Volatile
    private var bridgeImportRunning = false
    private val refreshRunnable: Runnable = object : Runnable {
        override fun run() {
            renderOverlayText()
            handler.postDelayed(this, REFRESH_INTERVAL_MS)
        }
    }
    private val bridgeImportRunnable: Runnable = object : Runnable {
        override fun run() {
            importDebugDataInBackground()
        }
    }

    private var windowManager: WindowManager? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var overlayTextView: TextView? = null
    private var overlayRoot: View? = null
    private var store: DpisConfigStore? = null
    private var gestureDetector: GestureDetector? = null
    private var suppressLongPress = false
    private var maxPointerCountDuringGesture = 1

    override fun onCreate() {
        super.onCreate()
        store = DpisApplication.getConfigStore()
        if (store == null) {
            store = ConfigStoreFactory.createLocalUiModuleConfigStore(
                this,
                DpisApplication.xposedService
            )
        }
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager?
        if (windowManager == null) {
            stopSelf()
            return
        }
        bridgeThread = HandlerThread("DPIS-font-debug-bridge")
        bridgeThread!!.start()
        bridgeHandler = Handler(bridgeThread!!.looper)
        createOverlayView()
        handler.post(refreshRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (overlayRoot == null && windowManager != null) {
            createOverlayView()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(refreshRunnable)
        if (bridgeHandler != null) {
            bridgeHandler!!.removeCallbacksAndMessages(null)
            bridgeHandler = null
        }
        if (bridgeThread != null) {
            bridgeThread!!.quitSafely()
            bridgeThread = null
        }
        if (windowManager != null && overlayRoot != null) {
            try {
                windowManager!!.removeView(overlayRoot)
            } catch (ignored: Throwable) {
            }
        }
        overlayRoot = null
        overlayTextView = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createOverlayView() {
        overlayTextView = TextView(this)
        overlayTextView!!.textSize = 11f
        overlayTextView!!.setTextColor(Color.WHITE)
        overlayTextView!!.setPadding(dp(10), dp(8), dp(10), dp(8))
        overlayTextView!!.maxWidth = dp(280)
        overlayTextView!!.setTypeface(Typeface.MONOSPACE)

        val bg = GradientDrawable()
        bg.setColor(-0x33eeeeef)
        bg.cornerRadius = dp(12).toFloat()
        bg.setStroke(dp(1), 0x66FFFFFF)
        overlayTextView!!.background = bg

        overlayRoot = overlayTextView
        gestureDetector = GestureDetector(this, OverlayGestureListener())
        gestureDetector!!.isLongpressEnabled = true
        overlayRoot!!.setOnTouchListener(DragTouchListener())

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            (WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL),
            PixelFormat.TRANSLUCENT
        )
        layoutParams!!.gravity = Gravity.TOP or Gravity.END
        layoutParams!!.x = dp(12)
        layoutParams!!.y = dp(120)

        windowManager!!.addView(overlayRoot, layoutParams)
        renderOverlayText()
    }

    private fun cycleWindowMode() {
        if (store == null) {
            return
        }
        val current = store!!.fontDebugSelectedWindow
        val next: Int
        if (current == FontDebugStatsStore.WINDOW_5S) {
            next = FontDebugStatsStore.WINDOW_30S
        } else if (current == FontDebugStatsStore.WINDOW_30S) {
            next = FontDebugStatsStore.WINDOW_ALL
        } else {
            next = FontDebugStatsStore.WINDOW_5S
        }
        store!!.setFontDebugSelectedWindow(next)
        renderOverlayText()
    }

    private fun cycleGroupMode() {
        if (store == null) {
            return
        }
        val current = store!!.fontDebugSelectedMode
        val next = if (current == FontDebugStatsStore.MODE_CHAIN)
            FontDebugStatsStore.MODE_CHAIN_VIEW
        else
            FontDebugStatsStore.MODE_CHAIN
        store!!.setFontDebugSelectedMode(next)
        renderOverlayText()
    }

    private fun renderOverlayText() {
        if (overlayTextView == null) {
            return
        }
        scheduleBridgeImportIfNeeded()
        val preferences = FontDebugStatsStore.getPreferences(this)
        val eventTotal = preferences.getInt(FontDebugStatsStore.KEY_EVENT_TOTAL, 0)
        val updatedAt = preferences.getLong(FontDebugStatsStore.KEY_UPDATED_AT, 0L)
        val mode =
            if (store != null) store!!.fontDebugSelectedMode else FontDebugStatsStore.MODE_CHAIN
        val window =
            if (store != null) store!!.fontDebugSelectedWindow else FontDebugStatsStore.WINDOW_ALL
        val now = System.currentTimeMillis()

        val statsKey = FontDebugStatsSchema.statsKeyFor(mode, window)
        var statsText: String = preferences.getString(statsKey, FontDebugStatsSchema.NO_DATA_TEXT)!!
        if (isWindowExpired(window, updatedAt, now)) {
            statsText = FontDebugStatsSchema.NO_DATA_TEXT
        }
        val hasFontStats = FontDebugStatsSchema.isNonEmptyStatsText(statsText)
        var unitBreakdown: String = buildUnitBreakdownFromStats(statsText)
        if ("unit: 0=0 1=0 2=0" == unitBreakdown) {
            unitBreakdown = preferences.getString(
                FontDebugStatsStore.KEY_UNIT_BREAKDOWN_5S,
                "unit: 0=0 1=0 2=0"
            )!!
        }
        val topLimit = if (store != null) max(
            3,
            store!!.getDebugInt(FontDebugStatsStore.KEY_FONT_DEBUG_OVERLAY_TOP_LIMIT, 3)
        ) else
            3
        val lines: Array<String?> =
            (if (statsText == null) FontDebugStatsSchema.NO_DATA_TEXT else statsText).split("\n".toRegex())
                .dropLastWhile { it.isEmpty() }.toTypedArray()
        val top = StringBuilder()
        if (hasFontStats) {
            val limit = min(topLimit, lines.size)
            for (i in 0..<limit) {
                top.append(lines[i])
                if (i < limit - 1) {
                    top.append('\n')
                }
            }
        } else {
            top.append("等待字体链路命中（需开启字体调节）")
        }

        val modeText = if (mode == FontDebugStatsStore.MODE_CHAIN) "链路" else "链路+视图"
        val windowText = when (window) {
            FontDebugStatsStore.WINDOW_5S -> "5秒"
            FontDebugStatsStore.WINDOW_30S -> "30秒"
            else -> "累计"
        }
        val viewportSummary = if (store != null)
            store!!.getDebugString(
                FontDebugStatsStore.KEY_VIEWPORT_DEBUG_SUMMARY,
                FontDebugStatsSchema.NO_VIEWPORT_TEXT
            )
        else
            FontDebugStatsSchema.NO_VIEWPORT_TEXT
        val viewportSection: String = formatViewportSection(viewportSummary)
        val fontModeText = resolveFontModeText(viewportSummary)
        val hasViewportSummary = FontDebugStatsSchema.isViewportSignal(viewportSummary)
        val loggingEnabled = store == null || store!!.isGlobalLogEnabled()
        val loggingNotice = if (loggingEnabled)
            ""
        else
            "日志输出已关闭，字体统计暂停\n"
        var body = String.format(
            Locale.US,
            "字体调试  %s · %s · Top%d\n%sDP/视口: %s\n字体模式: %s\n字体事件: %d\n%s\n\nTOP链路:\n%s\n\n点按:切窗口  双击:切Top  长按:切分组  双指长按:清空",
            windowText,
            modeText,
            topLimit,
            loggingNotice,
            viewportSection,
            fontModeText,
            eventTotal,
            unitBreakdown,
            top
        )
        if (!hasFontStats) {
            val reason =
                FontDebugDataDiagnostics.resolveNoDataReason(store, preferences)
            if (reason != NoDataReason.NONE) {
                body = String.format(
                    Locale.US,
                    "字体调试 无数据\n原因: %s\n%s\nDP/视口: %s\n字体模式: %s\n\n点按:切窗口  长按:切分组  双指长按:清空",
                    reasonTitle(reason),
                    reasonHint(reason),
                    viewportSection,
                    fontModeText
                )
            } else if (updatedAt <= 0L && eventTotal <= 0 && !hasViewportSummary) {
                body = "字体调试 初始化中\n点按:切窗口  长按:切分组  双指长按:清空"
            }
        }
        overlayTextView!!.text = body
    }

    private fun scheduleBridgeImportIfNeeded() {
        val backgroundHandler = bridgeHandler
        if (backgroundHandler == null || bridgeImportRunning) {
            return
        }
        val now = System.currentTimeMillis()
        if (now - lastBridgeImportAt < BRIDGE_IMPORT_INTERVAL_MS
            && now - lastLogcatImportAt < LOGCAT_IMPORT_INTERVAL_MS
        ) {
            return
        }
        bridgeImportRunning = true
        backgroundHandler.post(bridgeImportRunnable)
    }

    private fun importDebugDataInBackground() {
        val now = System.currentTimeMillis()
        try {
            if (now - lastBridgeImportAt >= BRIDGE_IMPORT_INTERVAL_MS) {
                FontDebugStatsFileBridge.importIfNewer(this)
                lastBridgeImportAt = now
            }
            if (now - lastLogcatImportAt >= LOGCAT_IMPORT_INTERVAL_MS) {
                val imported = FontDebugLogcatBridge.importRecent(this)
                lastLogcatImportAt =
                    if (imported) now else now + LOGCAT_FAILURE_RETRY_MS - LOGCAT_IMPORT_INTERVAL_MS
            }
        } finally {
            handler.post(object : Runnable {
                override fun run() {
                    bridgeImportRunning = false
                }
            })
        }
    }

    private fun resolveFontModeText(viewportSummary: String?): String {
        if (store == null) {
            return "未知"
        }
        val packageName: String? = parsePackageNameFromViewportSummary(viewportSummary)
        if (packageName == null || packageName.isBlank()) {
            return "未知（无目标包）"
        }
        val mode = store!!.getTargetFontApplyMode(packageName)
        return when (FontApplyMode.normalize(mode)) {
            FontApplyMode.SYSTEM_EMULATION -> "系统"
            FontApplyMode.FIELD_REWRITE -> "兼容"
            else -> "关闭"
        }
    }

    private fun clearDebugStatsData() {
        val preferences = FontDebugStatsStore.getPreferences(this)
        FontDebugStatsStore.clearStats(preferences)
        Toast.makeText(this, getString(R.string.font_debug_clear_done), Toast.LENGTH_SHORT).show()
        renderOverlayText()
    }

    private fun dp(value: Int): Int {
        val density = resources.displayMetrics.density
        return Math.round(value * density)
    }

    private inner class DragTouchListener : OnTouchListener {
        private var startX = 0
        private var startY = 0
        private var touchStartX = 0f
        private var touchStartY = 0f
        private var dragging = false

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            if (gestureDetector != null) {
                gestureDetector!!.onTouchEvent(event)
            }
            if (layoutParams == null || windowManager == null) {
                return false
            }
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = layoutParams!!.x
                    startY = layoutParams!!.y
                    touchStartX = event.rawX
                    touchStartY = event.rawY
                    dragging = false
                    suppressLongPress = false
                    maxPointerCountDuringGesture = 1
                    return true
                }

                MotionEvent.ACTION_POINTER_DOWN -> {
                    maxPointerCountDuringGesture =
                        max(maxPointerCountDuringGesture, event.pointerCount)
                    return true
                }

                MotionEvent.ACTION_MOVE -> {
                    maxPointerCountDuringGesture =
                        max(maxPointerCountDuringGesture, event.pointerCount)
                    val dx = Math.round(event.rawX - touchStartX)
                    val dy = Math.round(event.rawY - touchStartY)
                    if (abs(dx) > dp(4) || abs(dy) > dp(4)) {
                        dragging = true
                        suppressLongPress = true
                    }
                    if (dragging) {
                        layoutParams!!.x = startX - dx
                        layoutParams!!.y = startY + dy
                        windowManager!!.updateViewLayout(overlayRoot, layoutParams)
                        return true
                    }
                    return false
                }

                MotionEvent.ACTION_UP -> {
                    if (!dragging) {
                        v.performClick()
                    }
                    suppressLongPress = false
                    maxPointerCountDuringGesture = 1
                    return true
                }

                MotionEvent.ACTION_CANCEL -> {
                    suppressLongPress = false
                    maxPointerCountDuringGesture = 1
                    return true
                }

                else -> return true
            }
        }
    }

    private inner class OverlayGestureListener : SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            cycleWindowMode()
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            if (suppressLongPress) {
                return
            }
            if (maxPointerCountDuringGesture >= 2) {
                clearDebugStatsData()
                return
            }
            cycleGroupMode()
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            cycleTopLimit()
            return true
        }
    }

    private fun cycleTopLimit() {
        if (store == null) {
            return
        }
        val current = max(
            3, store!!.getDebugInt(
                FontDebugStatsStore.KEY_FONT_DEBUG_OVERLAY_TOP_LIMIT, 3
            )
        )
        val next = if (current >= 10) 3 else 10
        store!!.setDebugInt(FontDebugStatsStore.KEY_FONT_DEBUG_OVERLAY_TOP_LIMIT, next)
        renderOverlayText()
    }

    companion object {
        private const val REFRESH_INTERVAL_MS = 500L
        private const val BRIDGE_IMPORT_INTERVAL_MS = 2000L
        private const val LOGCAT_IMPORT_INTERVAL_MS = 5000L
        private const val LOGCAT_FAILURE_RETRY_MS = 30000L
        private const val WINDOW_5S_STALE_MS = 5000L
        private const val WINDOW_30S_STALE_MS = 30000L
        private val UNIT_LINE_PATTERN: Pattern =
            Pattern.compile("^\\s*(\\d+)\\s+text-size-unit-(\\d)\\b.*$")
        private val VIEWPORT_PACKAGE_PATTERN: Pattern = Pattern.compile("^视口\\s++([^|\\s]++).*+")

        private fun formatViewportSection(viewportSummary: String?): String {
            if (!FontDebugStatsSchema.isViewportSignal(viewportSummary)) {
                return "暂无"
            }
            val normalized = viewportSummary!!.trim { it <= ' ' }
            if (!normalized.startsWith("视口 ")) {
                return normalized
            }
            val parts: Array<String?> =
                normalized.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (parts.size < 3) {
                return normalized.substring(3).trim { it <= ' ' }
            }
            val pkg = parts[0]!!.replaceFirst("^视口\\s+".toRegex(), "").trim { it <= ' ' }
            val dpMode = parts[1]!!.trim { it <= ' ' }
            val lines = StringBuilder()
            lines.append("包名: ").append(pkg)
                .append('\n')
                .append("DP模式: ").append(dpMode)
            for (i in 2..<parts.size) {
                val part = parts[i]!!.trim { it <= ' ' }
                if (part.isEmpty()) {
                    continue
                }
                lines.append('\n').append(part)
            }
            return lines.toString()
        }

        private fun parsePackageNameFromViewportSummary(viewportSummary: String?): String? {
            if (viewportSummary == null || viewportSummary.isBlank()) {
                return null
            }
            val matcher: Matcher =
                VIEWPORT_PACKAGE_PATTERN.matcher(viewportSummary.trim { it <= ' ' })
            if (!matcher.matches()) {
                return null
            }
            val pkg = matcher.group(1)
            return if (pkg == null || pkg.isBlank()) null else pkg.trim { it <= ' ' }
        }

        private fun reasonTitle(reason: NoDataReason): String {
            return when (reason) {
                NoDataReason.SCOPE_MISSING -> "作用域缺失"
                NoDataReason.NOT_INJECTED -> "未注入"
                NoDataReason.NO_EVENTS -> "无事件"
                else -> "未知"
            }
        }

        private fun reasonHint(reason: NoDataReason): String {
            return when (reason) {
                NoDataReason.SCOPE_MISSING -> "未配置目标应用，请先在应用列表加入作用域"
                NoDataReason.NOT_INJECTED -> "目标进程未加载模块，检查 LSPosed 启用状态并重启目标应用"
                NoDataReason.NO_EVENTS -> "已注入但未命中字体链路，进入文字页面或切到累计窗口观察"
                else -> "请稍后重试"
            }
        }

        private fun isWindowExpired(window: Int, updatedAt: Long, now: Long): Boolean {
            if (updatedAt <= 0L || now <= updatedAt) {
                return false
            }
            val elapsedMs = now - updatedAt
            if (window == FontDebugStatsStore.WINDOW_5S) {
                return elapsedMs > WINDOW_5S_STALE_MS
            }
            if (window == FontDebugStatsStore.WINDOW_30S) {
                return elapsedMs > WINDOW_30S_STALE_MS
            }
            return false
        }

        private fun buildUnitBreakdownFromStats(statsText: String?): String {
            if (statsText == null || statsText.isEmpty()) {
                return "unit: 0=0 1=0 2=0"
            }
            var unit0 = 0
            var unit1 = 0
            var unit2 = 0
            val lines: Array<String?> =
                statsText.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (line in lines) {
                if (line == null || line.isEmpty()) {
                    continue
                }
                val matcher: Matcher = UNIT_LINE_PATTERN.matcher(line)
                if (!matcher.matches()) {
                    continue
                }
                val count: Int
                val unit: Int
                try {
                    count = matcher.group(1).toInt()
                    unit = matcher.group(2).toInt()
                } catch (ignored: NumberFormatException) {
                    continue
                }
                if (count <= 0) {
                    continue
                }
                if (unit == 0) {
                    unit0 += count
                } else if (unit == 1) {
                    unit1 += count
                } else if (unit == 2) {
                    unit2 += count
                }
            }
            return "unit: 0=" + unit0 + " 1=" + unit1 + " 2=" + unit2
        }
    }
}
