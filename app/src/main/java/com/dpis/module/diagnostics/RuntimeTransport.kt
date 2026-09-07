package com.dpis.module.diagnostics

import com.dpis.module.diagnostics.ProcessPerformance.RouteSnapshot
import com.dpis.module.root.RootAppProcessLauncher.ShellResult
import com.dpis.module.runtime.SecureProcessLauncher
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.Volatile

object RuntimeTransport {
    private const val DIRECTORY = "/data/local/tmp/dpis-feedback-diagnostic"
    private val MARKER_FILE = DIRECTORY + "/active-session"
    private const val SESSION_PROPERTY = "debug.dpis.diag.session"
    private const val EVENT_FILE_NAME = "runtime-events.jsonl"
    private val MAX_EXPORT_BYTES = 256L * 1024L

    @Volatile
    private var activeSession: Session? = null

    @Volatile
    private var lastMarkerCheckMillis: Long = 0

    @Volatile
    private var remoteSession: RemoteSession? = null
    private const val MAX_PENDING_LINES = 8192
    private val PENDING_LINES: LinkedBlockingQueue<PendingLine> = LinkedBlockingQueue<PendingLine>(
        MAX_PENDING_LINES
    )
    private val WRITER_LOCK = Any()
    private val DROPPED_LINES = AtomicLong()

    @Volatile
    private var writerThread: Thread? = null
    private var linesBeingWritten = 0

    @JvmStatic
    fun start(packageName: String?, shellRunner: ShellRunner?): Status {
        val sessionId = UUID.randomUUID().toString()
        val eventPath = DIRECTORY + "/" + sessionId + "-" + EVENT_FILE_NAME
        val runner = if (shellRunner != null)
            shellRunner
        else ShellRunner { command -> runSuCommand(command) }
        val result = runner.run(
            ("mkdir -p " + shellQuote(DIRECTORY)
                    + " && chmod 755 " + shellQuote(DIRECTORY)
                    + " && : > " + shellQuote(eventPath)
                    + " && chmod 666 " + shellQuote(eventPath)
                    + " && printf %s " + shellQuote(eventPath)
                    + " > " + shellQuote(MARKER_FILE)
                    + " && chmod 644 " + shellQuote(MARKER_FILE)
                    + " && setprop " + shellQuote(SESSION_PROPERTY) + " " + shellQuote(sessionId))
        )
        if (result.code() != 0) {
            activeSession = Session(
                "", false,
                "runtime transport unavailable: " + compact(result.output())
            )
            return Status.unavailable(activeSession!!.reason)
        }
        activeSession = Session(eventPath, true, "")
        remoteSession = null
        lastMarkerCheckMillis = 0L
        return Status.available(eventPath)
    }

    @JvmStatic
    fun stopSnapshot(shellRunner: ShellRunner?): Snapshot {
        flushPendingWrites()
        val session = activeSession
        activeSession = null
        remoteSession = null
        lastMarkerCheckMillis = 0L
        if (session == null) {
            return Snapshot.unavailable("runtime transport unavailable: not started")
        }
        if (!session.available) {
            return Snapshot.unavailable(session.reason)
        }
        val runner = if (shellRunner != null)
            shellRunner
        else ShellRunner { command -> runSuCommand(command) }
        val readResult = runner.run(
            ("cat "
                    + shellQuote(session.eventPath)
                    + " 2>/dev/null | head -c "
                    + MAX_EXPORT_BYTES
                    + "; rm -f "
                    + shellQuote(session.eventPath)
                    + " "
                    + shellQuote(MARKER_FILE)
                    + "; setprop " + shellQuote(SESSION_PROPERTY) + " ''")
        )
        if (readResult.code() != 0) {
            return Snapshot.unavailable(
                "runtime transport unavailable: " + compact(readResult.output())
            )
        }
        return Snapshot.available(parseEvents(readResult.output()))
    }

    @JvmStatic
    fun peekSnapshot(shellRunner: ShellRunner?): Snapshot {
        val session = activeSession
        if (session == null) {
            return Snapshot.unavailable("runtime transport unavailable: not started")
        }
        if (!session.available) {
            return Snapshot.unavailable(session.reason)
        }
        val runner = if (shellRunner != null)
            shellRunner
        else ShellRunner { command -> runSuCommand(command) }
        val readResult = runner.run(
            ("cat "
                    + shellQuote(session.eventPath)
                    + " 2>/dev/null | head -c "
                    + MAX_EXPORT_BYTES)
        )
        if (readResult.code() != 0) {
            return Snapshot.unavailable(
                "runtime transport unavailable: " + compact(readResult.output())
            )
        }
        return Snapshot.available(parseEvents(readResult.output()))
    }

    @JvmStatic
    fun cancel(shellRunner: ShellRunner?) {
        val session = activeSession
        activeSession = null
        remoteSession = null
        lastMarkerCheckMillis = 0L
        if (session == null || !session.available) {
            return
        }
        val runner = if (shellRunner != null)
            shellRunner
        else ShellRunner { command -> runSuCommand(command) }
        runner.run(
            ("rm -f " + shellQuote(session.eventPath) + " "
                    + shellQuote(MARKER_FILE)
                    + "; setprop " + shellQuote(SESSION_PROPERTY) + " ''")
        )
    }

    @JvmStatic
    fun record(category: String?, stage: String?, packageName: String?, message: String?) {
        record(category, "", stage, packageName, message)
    }

    @JvmStatic
    fun record(
        category: String?,
        route: String?,
        stage: String?,
        packageName: String?,
        message: String?
    ) {
        record(category, route, "", stage, packageName, message)
    }

    @JvmStatic
    fun record(
        category: String?,
        route: String?,
        routeName: String?,
        stage: String?,
        packageName: String?,
        message: String?
    ) {
        val local = activeSession
        if (local != null && local.available) {
            enqueueLine(
                local.eventPath,
                toLine(category, route, routeName, stage, packageName, message)
            )
            return
        }
        val remote = resolveRemoteSession()
        if (remote != null) {
            enqueueLine(
                remote.eventPath,
                toLine(category, route, routeName, stage, packageName, message)
            )
        }
    }

    @JvmStatic
    fun flushForTest() {
        flushPendingWrites()
    }

    /**
     * Publishes a compact process-local performance aggregate. Unlike
     * [.record], this is emitted at a
     * bounded cadence by the target process and must not be called per hook
     * callback.
     */
    @JvmStatic
    fun recordPerformanceSnapshot(
        packageName: String?,
        processName: String?,
        pid: Int,
        routes: MutableMap<String?, RouteSnapshot?>?
    ) {
        if (routes == null || routes.isEmpty()) {
            return
        }
        val message = StringBuilder()
        message.append("process=").append(valueOrDefault(processName, "unknown"))
            .append(",pid=").append(pid)
        val droppedLines = DROPPED_LINES.getAndSet(0L)
        if (droppedLines > 0L) {
            message.append(",transportDroppedLines=").append(droppedLines)
        }
        for (entry
        in routes.entries) {
            val snapshot: RouteSnapshot = entry.value!!
            message.append(";route=").append(entry.key)
                .append(",calls=").append(snapshot.calls)
                .append(",applied=").append(snapshot.applied)
                .append(",skipped=").append(snapshot.skipped)
                .append(",kept=").append(snapshot.kept)
                .append(",measuredCalls=").append(snapshot.measuredCalls)
                .append(",p50Us=").append(snapshot.p50Us)
                .append(",p95Us=").append(snapshot.p95Us)
                .append(",p99Us=").append(snapshot.p99Us)
                .append(",maxUs=").append(snapshot.maxUs)
            if (!snapshot.skipReasons.isEmpty()) {
                message.append(",skipReasons=")
                var first = true
                for (reason in snapshot.skipReasons.entries) {
                    if (!first) {
                        message.append('|')
                    }
                    message.append(reason.key).append(':').append(reason.value)
                    first = false
                }
            }
        }
        record("performance", "runtime", "aggregate", packageName, message.toString())
        RuntimeBridgeEvents.emitPerformance(message.toString())
    }

    @JvmStatic
    fun statusForTest(): Status {
        val session = activeSession
        if (session == null) {
            return Status.unavailable("runtime transport unavailable: not started")
        }
        return if (session.available) Status.available(session.eventPath) else Status.unavailable(
            session.reason
        )
    }

    @JvmStatic
    fun activeEventPath(): String {
        val session = activeSession
        if (session != null && session.available) {
            return session.eventPath
        }
        val remote = resolveRemoteSession()
        return if (remote != null) remote.eventPath else ""
    }

    @JvmStatic
    val isCaptureActive: Boolean
        get() {
            val session = activeSession
            if (session != null && session.available) {
                return true
            }
            return resolveRemoteSession() != null
        }

    /**
     * Returns a compact process-entry diagnostic marker without writing any
     * event. App-process module entry calls this once, before hook hot paths,
     * so an active session can prove whether its marker/property was visible
     * to the injected process.
     */
    @JvmStatic
    fun activeSessionDiscoveryDetail(): String {
        val local = activeSession
        if (local != null && local.available) {
            return "source=local-session"
        }
        val markerPath = readMarkerEventPath()
        val propertySessionId = readSystemProperty(SESSION_PROPERTY)
        val remote = resolveRemoteSession()
        if (remote == null) {
            return ""
        }
        return ("source=remote-session"
                + ", markerVisible=" + !markerPath.isBlank() + ", propertyVisible=" + !propertySessionId.isBlank())
    }

    @JvmStatic
    fun writeSelfTestEvent(
        packageName: String?,
        message: String?,
        shellRunner: ShellRunner?
    ): Boolean {
        val eventPath = activeEventPath()
        if (eventPath.isBlank()) {
            return false
        }
        val runner = if (shellRunner != null)
            shellRunner
        else ShellRunner { command -> runSuCommand(command) }
        val line = toLine(
            "runtime", "self_test", "self_test", "self_test",
            packageName, message
        )
        val result = runner.run(
            "printf %s\\\\n " + shellQuote(line) + " >> " + shellQuote(eventPath)
        )
        return result.code() == 0
    }

    private fun resolveRemoteSession(): RemoteSession? {
        val now = System.currentTimeMillis()
        val cached = remoteSession
        if (cached != null && now - lastMarkerCheckMillis < 1000L) {
            return if (cached.available) cached else null
        }
        lastMarkerCheckMillis = now
        val markerEventPath = readMarkerEventPath()
        if (!markerEventPath.isBlank()) {
            remoteSession = RemoteSession.connected(markerEventPath)
            return remoteSession
        }
        val sessionId = readSystemProperty(SESSION_PROPERTY)
        if (!sessionId.isBlank() && sessionId.matches("[0-9a-fA-F-]{36}".toRegex())) {
            remoteSession = RemoteSession.connected(
                DIRECTORY + "/" + sessionId + "-" + EVENT_FILE_NAME
            )
            return remoteSession
        }
        remoteSession = RemoteSession.unavailable()
        return null
    }

    private fun readMarkerEventPath(): String {
        val marker = File(MARKER_FILE)
        if (!marker.isFile) {
            return ""
        }
        try {
            val eventPath = String(
                Files.readAllBytes(marker.toPath()),
                StandardCharsets.UTF_8
            ).trim { it <= ' ' }
            return if (eventPath.startsWith(DIRECTORY + "/")) eventPath else ""
        } catch (ignored: IOException) {
            // The property fallback remains available when the marker is hidden.
            return ""
        } catch (ignored: RuntimeException) {
            return ""
        }
    }

    private fun readSystemProperty(name: String?): String {
        try {
            val systemProperties = Class.forName("android.os.SystemProperties")
            val get = systemProperties.getDeclaredMethod(
                "get", String::class.java, String::class.java
            )
            try {
                get.isAccessible = true
            } catch (ignored: RuntimeException) {
                // LSPosed hosts may already expose the method. Keep the
                // invocation attempt even when hidden-API access cannot be
                // relaxed by this process.
            }
            val value = get.invoke(null, name, "")
            return if (value != null) value.toString().trim { it <= ' ' } else ""
        } catch (ignored: Throwable) {
            // A target process may be blocked from reading shell_data_file
            // markers by SELinux. The debug property is therefore the durable
            // remote-session discovery path and must fail closed silently.
            return ""
        }
    }

    private fun enqueueLine(eventPath: String?, line: String?) {
        if (!PENDING_LINES.offer(PendingLine(eventPath, line + "\n"))) {
            DROPPED_LINES.incrementAndGet()
            return
        }
        ensureWriter()
    }

    private fun ensureWriter() {
        if (writerThread != null) {
            return
        }
        synchronized(WRITER_LOCK) {
            if (writerThread != null) {
                return
            }
            val thread = Thread(Runnable {
                while (true) {
                    try {
                        val first = PENDING_LINES.take()
                        val batches = LinkedHashMap<String?, StringBuilder>()
                        appendToBatch(batches, first)
                        for (i in 1..63) {
                            val next = PENDING_LINES.poll()
                            if (next == null) {
                                break
                            }
                            appendToBatch(batches, next)
                        }
                        val batchLineCount = countLines(batches)
                        synchronized(WRITER_LOCK) {
                            linesBeingWritten += batchLineCount
                        }
                        try {
                            for (entry in batches.entries) {
                                try {
                                    Files.write(
                                        File(entry.key).toPath(),
                                        entry.value.toString().toByteArray(StandardCharsets.UTF_8),
                                        StandardOpenOption.CREATE,
                                        StandardOpenOption.APPEND
                                    )
                                } catch (ignored: IOException) {
                                    // Runtime diagnostics must never affect hooked app behavior.
                                } catch (ignored: RuntimeException) {
                                }
                            }
                        } finally {
                            synchronized(WRITER_LOCK) {
                                linesBeingWritten -= batchLineCount
                                (WRITER_LOCK as Object).notifyAll()
                            }
                        }
                    } catch (ignored: InterruptedException) {
                        Thread.currentThread().interrupt()
                        return@Runnable
                    } catch (ignored: Throwable) {
                        // A writer failure must never affect the target process.
                    }
                }
            }, "DPIS-diagnostic-transport")
            thread.isDaemon = true
            writerThread = thread
            thread.start()
        }
    }

    private fun appendToBatch(
        batches: MutableMap<String?, StringBuilder>,
        line: PendingLine
    ) {
        val builder =
            batches.computeIfAbsent(line.eventPath) { ignored: String? -> StringBuilder() }
        builder.append(line.content)
    }

    private fun countLines(batches: MutableMap<String?, StringBuilder>): Int {
        var count = 0
        for (batch in batches.values) {
            for (i in 0..<batch.length) {
                if (batch.get(i) == '\n') {
                    count++
                }
            }
        }
        return count
    }

    private fun flushPendingWrites() {
        ensureWriter()
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2L)
        synchronized(WRITER_LOCK) {
            while ((!PENDING_LINES.isEmpty() || linesBeingWritten > 0)
                && System.nanoTime() < deadline
            ) {
                try {
                    (WRITER_LOCK as Object).wait(25L)
                } catch (ignored: InterruptedException) {
                    Thread.currentThread().interrupt()
                    return
                }
            }
        }
    }

    private fun toLine(
        category: String?,
        route: String?,
        routeName: String?,
        stage: String?,
        packageName: String?,
        message: String?
    ): String {
        val now = System.currentTimeMillis()
        return ("{\"timestampMillis\":" + now
                + ",\"displayTime\":\"" + jsonEscape(formatTime(now)) + "\""
                + ",\"source\":\"runtime-transport\""
                + ",\"category\":\"" + jsonEscape(valueOrDefault(category, "runtime")) + "\""
                + ",\"route\":\"" + jsonEscape(valueOrDefault(route, "")) + "\""
                + ",\"routeName\":\"" + jsonEscape(valueOrDefault(routeName, "")) + "\""
                + ",\"stage\":\"" + jsonEscape(valueOrDefault(stage, "event")) + "\""
                + ",\"package\":\"" + jsonEscape(valueOrDefault(packageName, "unknown")) + "\""
                + ",\"message\":\"" + jsonEscape(sanitize(message)) + "\""
                + "}")
    }

    private fun parseEvents(raw: String?): MutableList<String?> {
        if (raw == null || raw.isBlank()) {
            return mutableListOf<String?>()
        }
        val events: MutableList<String?> = ArrayList<String?>()
        for (line in raw.split("\\R".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            val timestampMillis = readLongField(line, "timestampMillis", 0L)
            val displayTime = readStringField(line, "displayTime")
            val category = readStringField(line, "category")
            val route = readStringField(line, "route")
            val routeName = readStringField(line, "routeName")
            val stage = readStringField(line, "stage")
            val packageName = readStringField(line, "package")
            val message = readStringField(line, "message")
            if (timestampMillis <= 0L || message.isBlank()) {
                continue
            }
            events.add(
                (valueOrDefault(displayTime, formatTime(timestampMillis))
                        + " source=runtime-transport"
                        + " category=" + valueOrDefault(category, "runtime")
                        + routePart(route)
                        + routeNamePart(routeName)
                        + " stage=" + valueOrDefault(stage, "event")
                        + " package=" + valueOrDefault(packageName, "unknown")
                        + " message=" + message)
            )
        }
        events.sortWith(compareBy { it.orEmpty() })
        return events
    }

    private fun routePart(route: String?): String {
        val normalized = valueOrDefault(route, "")
        return if (normalized.isEmpty()) "" else " route=" + normalized
    }

    private fun routeNamePart(routeName: String?): String {
        val normalized = valueOrDefault(routeName, "")
        return if (normalized.isEmpty()) "" else " routeName=" + normalized
    }

    private fun runSuCommand(command: String?): ShellResult {
        var process: Process? = null
        try {
            process = SecureProcessLauncher.start("su", "-c", command)
            val output = StringBuilder()
            BufferedReader(
                InputStreamReader(process.inputStream, StandardCharsets.UTF_8)
            ).use { reader ->
                BufferedReader(
                    InputStreamReader(process.errorStream, StandardCharsets.UTF_8)
                ).use { errReader ->
                    readAll(reader, output)
                    readAll(errReader, output)
                }
            }
            return ShellResult(process.waitFor(), output.toString())
        } catch (exception: IOException) {
            return ShellResult(-1, exceptionMessage(exception))
        } catch (exception: InterruptedException) {
            Thread.currentThread().interrupt()
            return ShellResult(-1, exceptionMessage(exception))
        } finally {
            if (process != null) {
                process.destroy()
            }
        }
    }

    @Throws(IOException::class)
    private fun readAll(reader: BufferedReader, output: StringBuilder) {
        var line: String?
        while ((reader.readLine().also { line = it }) != null) {
            if (output.length > 0) {
                output.append('\n')
            }
            output.append(line)
        }
    }

    private fun readStringField(line: String, fieldName: String): String {
        val prefix = "\"" + fieldName + "\":\""
        var start = line.indexOf(prefix)
        if (start < 0) {
            return ""
        }
        start += prefix.length
        val value = StringBuilder()
        var escaped = false
        for (i in start..<line.length) {
            val ch = line.get(i)
            if (escaped) {
                value.append(ch)
                escaped = false
            } else if (ch == '\\') {
                escaped = true
            } else if (ch == '"') {
                break
            } else {
                value.append(ch)
            }
        }
        return value.toString()
    }

    private fun readLongField(line: String, fieldName: String, fallback: Long): Long {
        val prefix = "\"" + fieldName + "\":"
        var start = line.indexOf(prefix)
        if (start < 0) {
            return fallback
        }
        start += prefix.length
        var end = start
        while (end < line.length && Character.isDigit(line.get(end))) {
            end++
        }
        try {
            return line.substring(start, end).toLong()
        } catch (exception: NumberFormatException) {
            return fallback
        }
    }

    private fun formatTime(millis: Long): String {
        return SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US).format(Date(millis))
    }

    private fun shellQuote(value: String): String {
        return "'" + value.replace("'", "'\\''") + "'"
    }

    private fun jsonEscape(value: String): String {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
    }

    private fun sanitize(value: String?): String {
        return if (value == null) "" else value.replace('\n', ' ').replace('\r', ' ')
            .trim { it <= ' ' }
    }

    private fun valueOrDefault(value: String?, fallback: String): String {
        val normalized = if (value != null) value.trim { it <= ' ' } else ""
        return if (normalized.isEmpty()) fallback else normalized
    }

    private fun compact(value: String?): String {
        val normalized = sanitize(value)
        return if (normalized.isEmpty()) "unknown" else normalized
    }

    private fun exceptionMessage(exception: Exception): String? {
        return if (exception.message != null)
            exception.message
        else
            exception.javaClass.simpleName
    }

    fun interface ShellRunner {
        fun run(command: String): ShellResult
    }

    private class PendingLine(val eventPath: String?, val content: String?)

    class Status private constructor(
        @JvmField val available: Boolean,
        path: String?,
        message: String?
    ) {
        @JvmField
        val path: String
        @JvmField
        val message: String

        init {
            this.path = if (path != null) path else ""
            this.message = if (message != null) message else ""
        }

        companion object {
            @JvmStatic
            fun available(path: String?): Status {
                return Status(true, path, "runtime transport available")
            }

            @JvmStatic
            fun unavailable(message: String?): Status {
                return Status(false, "", message)
            }
        }
    }

    class Snapshot private constructor(
        @JvmField val available: Boolean,
        events: MutableList<String?>?,
        note: String?
    ) {
        @JvmField
        val events: MutableList<String?>
        @JvmField
        val note: String

        init {
            this.events =
                if (events != null) ArrayList<String?>(events) else mutableListOf<String?>()
            this.note = if (note != null) note else ""
        }

        companion object {
            @JvmStatic
            fun available(events: MutableList<String?>?): Snapshot {
                return Snapshot(true, events, "")
            }

            @JvmStatic
            fun unavailable(note: String?): Snapshot {
                return Snapshot(false, mutableListOf<String?>(), note)
            }
        }
    }

    private class Session(eventPath: String?, val available: Boolean, reason: String?) {
        val eventPath: String
        val reason: String

        init {
            this.eventPath = if (eventPath != null) eventPath else ""
            this.reason = if (reason != null) reason else ""
        }
    }

    private class RemoteSession(val available: Boolean, eventPath: String?) {
        val eventPath: String

        init {
            this.eventPath = if (eventPath != null) eventPath else ""
        }

        companion object {
            fun connected(eventPath: String?): RemoteSession {
                return RemoteSession(true, eventPath)
            }

            fun unavailable(): RemoteSession {
                return RemoteSession(false, "")
            }
        }
    }
}
