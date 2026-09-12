package com.dpis.module.quirks.presentation

import android.app.Activity
import com.dpis.module.ui.presentation.MainComposeShellHost
import com.dpis.module.R
import com.dpis.module.ui.compose.ComposeMessageDialog
import java.util.function.Supplier

/** Shows the WeChat DPI explanation; Compose shell owns visibility when present. */
class WechatDpiHelp(
    private val activity: Activity,
    private val shell: Supplier<MainComposeShellHost?>,
) {
    fun show() {
        val host = shell.get()
        if (host != null) {
            host.showWechatDpiHelp()
            return
        }
        ComposeMessageDialog.show(
            activity,
            activity.getString(R.string.dialog_wechat_dpi_help_title),
            activity.getString(R.string.dialog_wechat_dpi_help_message),
            activity.getString(R.string.dialog_close_button),
        )
    }
}
