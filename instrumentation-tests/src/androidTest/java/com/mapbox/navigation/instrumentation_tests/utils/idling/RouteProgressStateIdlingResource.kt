package com.mapbox.navigation.instrumentation_tests.utils.idling

import androidx.test.espresso.IdlingResource
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.testing.ui.idling.NavigationIdlingResource

class RouteProgressStateIdlingResource(
    private val mapboxNavigation: MapboxNavigation,
    private val awaitedProgressState: RouteProgressState
) : NavigationIdlingResource() {

    private var currentRouteProgressState: RouteProgressState? = null

    override fun getName() = "RouteProgressStateIdlingResource"

    override fun isIdleNow() = currentRouteProgressState == awaitedProgressState

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
        currentRouteProgressState = null
        mapboxNavigation.registerRouteProgressObserver(object : RouteProgressObserver {
            override fun onRouteProgressChanged(routeProgress: RouteProgress) {
                currentRouteProgressState = routeProgress.currentState
                if (isIdleNow) {
                    mapboxNavigation.unregisterRouteProgressObserver(this)
                    callback?.onTransitionToIdle()
                }
            }
        })
    }
}
