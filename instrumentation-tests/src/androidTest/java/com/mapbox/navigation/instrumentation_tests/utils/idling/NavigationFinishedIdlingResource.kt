package com.mapbox.navigation.instrumentation_tests.utils.idling

import androidx.test.espresso.IdlingResource
import com.mapbox.navigation.testing.ui.idling.NavigationIdlingResource

class NavigationFinishedIdlingResource : NavigationIdlingResource() {

    private var callback: IdlingResource.ResourceCallback? = null
    private var isFinished = false

    override fun getName() = "NavigationFinishedIdlingResource"

    override fun isIdleNow() = isFinished

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
        this.callback = callback
    }

    fun navigationFinished() {
        isFinished = true
        callback?.onTransitionToIdle()
    }
}
