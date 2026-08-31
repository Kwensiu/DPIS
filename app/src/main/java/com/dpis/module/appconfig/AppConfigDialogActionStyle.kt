package com.dpis.module.appconfig

import android.content.res.ColorStateList

/** Captured button styling restored after dialog actions mutate the controls. */
open class AppConfigDialogActionStyle(
    @JvmField val defaultActionBgTint: ColorStateList?,
    @JvmField val defaultActionStrokeWidth: Int,
    @JvmField val defaultActionTextColor: Int,
)
