package com.mapbox.navigation.instrumentation_tests.utils

import android.content.Context
import androidx.annotation.IntegerRes
import com.mapbox.navigation.instrumentation_tests.R

fun readRawFileText(context: Context, @IntegerRes res: Int): String =
    context.resources.openRawResource(
        R.raw.route_response_dc_very_short
    ).bufferedReader().use { it.readText() }
