package com.mapbox.navigation.instrumentation_tests.utils

import androidx.test.platform.app.InstrumentationRegistry

fun runOnMainSync(runnable: Runnable) =
    InstrumentationRegistry.getInstrumentation().runOnMainSync(runnable)

fun runOnMainSync(fn: () -> Unit) =
    InstrumentationRegistry.getInstrumentation().runOnMainSync(fn)
