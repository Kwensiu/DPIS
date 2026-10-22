package com.dpis.module.runtime.transport

import android.os.Process
import com.dpis.module.runtime.systemserver.selection.PackageUidResolver

/** Only for app-process callers; system_server must pass the target user explicitly. */
object ProcessUser {
    fun currentUserId(): Int = checkNotNull(PackageUidResolver.userIdFromUid(Process.myUid())) {
        "Cannot resolve runtime process user"
    }
}
