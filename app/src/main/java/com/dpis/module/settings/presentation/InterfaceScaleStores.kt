package com.dpis.module.settings.presentation

import android.content.Context
import com.dpis.module.settings.InterfaceScaleStore
import com.dpis.module.settings.LegacyUiPreferenceKeys

fun interfaceScaleStore(context: Context): InterfaceScaleStore = InterfaceScaleStore(
    context.getSharedPreferences(InterfaceScaleStore.PREFS_NAME, Context.MODE_PRIVATE),
    context.getSharedPreferences(LegacyUiPreferenceKeys.GROUP, Context.MODE_PRIVATE),
)
