package com.dpis.module.updates.presentation

import android.content.Context
import com.dpis.module.settings.LegacyUiPreferenceKeys
import com.dpis.module.updates.StartupDisclaimerStore

fun startupDisclaimerStore(context: Context): StartupDisclaimerStore = StartupDisclaimerStore(
    context.getSharedPreferences(StartupDisclaimerStore.PREFS_NAME, Context.MODE_PRIVATE),
    context.getSharedPreferences(LegacyUiPreferenceKeys.GROUP, Context.MODE_PRIVATE),
)
