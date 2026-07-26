package com.dpis.module

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

class DpisTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader,
        className: String,
        context: Context
    ): Application = super.newApplication(
        cl,
        DpisTestApplication::class.java.name,
        context
    )
}
