package com.dpis.module.backup

internal object ConfigRestoreReadiness {
    fun runIfReady(isReady: () -> Boolean, restore: () -> Unit): Boolean {
        if (!isReady()) return false
        restore()
        return true
    }
}
