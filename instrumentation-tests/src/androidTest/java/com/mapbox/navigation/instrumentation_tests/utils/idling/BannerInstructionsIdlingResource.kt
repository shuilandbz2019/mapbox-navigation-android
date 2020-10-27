package com.mapbox.navigation.instrumentation_tests.utils.idling

import androidx.test.espresso.IdlingResource
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.BannerInstructionsObserver
import com.mapbox.navigation.testing.ui.idling.NavigationIdlingResource

class BannerInstructionsIdlingResource(
    private val mapboxNavigation: MapboxNavigation,
    private val expectedBannerInstructions: BannerInstructions
) : NavigationIdlingResource() {

    private var idle = false

    override fun getName() = "BannerInstructionsIdlingResource"

    override fun isIdleNow() = idle

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
        mapboxNavigation.registerBannerInstructionsObserver(object : BannerInstructionsObserver {
            override fun onNewBannerInstructions(bannerInstructions: BannerInstructions) {
                if (bannerInstructions == expectedBannerInstructions) {
                    mapboxNavigation.unregisterBannerInstructionsObserver(this)
                    idle = true
                    callback?.onTransitionToIdle()
                }
            }
        })
    }
}
