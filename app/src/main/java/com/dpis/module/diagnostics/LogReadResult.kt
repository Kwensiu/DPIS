package com.dpis.module.diagnostics

import android.content.Context
import com.dpis.module.R
import java.util.Locale

class LogReadResult @JvmOverloads constructor(
    val code: Int,
    val sourceLabel: String?,
    output: String?,
    error: String?,
    val sourceFilesPresent: Boolean = false,
    val validEntriesPresent: Boolean = false,
) {
    val output: String = output.orEmpty()
    val error: String = error.orEmpty()

    fun code() = code

    fun sourceLabel() = sourceLabel

    fun output() = output

    fun error() = error

    fun sourceFilesPresent() = sourceFilesPresent

    fun validEntriesPresent() = validEntriesPresent

    fun messageForEmptyState(context: Context): String {
        if (code == 0) return context.getString(R.string.log_empty_message)
        if (needsRootAccess()) return context.getString(R.string.log_lsposed_root_required_message)
        val reason = failureReason(context.getString(R.string.log_unknown_error))
        return context.getString(R.string.log_read_failed_message, reason)
    }

    fun failureReason(unknownError: String): String =
        error.takeUnless(String::isBlank) ?: output.takeUnless(String::isBlank) ?: unknownError

    fun needsRootAccess(): Boolean {
        val reason = (error + "\n" + output).lowercase(Locale.getDefault())
        return ROOT_ACCESS_ERRORS.any(reason::contains)
    }

    companion object {
        private val ROOT_ACCESS_ERRORS = listOf(
            "permission denied",
            "not allowed",
            "denied",
            "su: inaccessible",
            "su: not found",
            "can't execute",
            "no such file or directory",
            "root access",
        )
    }
}
