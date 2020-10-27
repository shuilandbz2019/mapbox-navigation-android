package com.mapbox.navigation.instrumentation_tests.ui.navigation_view

import android.text.SpannedString
import android.widget.TextView
import androidx.test.espresso.Espresso
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.activity.BasicNavigationViewActivity
import com.mapbox.navigation.instrumentation_tests.utils.MapboxNavigationRule
import com.mapbox.navigation.instrumentation_tests.utils.idling.BannerInstructionsIdlingResource
import com.mapbox.navigation.instrumentation_tests.utils.idling.NavigationViewInitIdlingResource
import com.mapbox.navigation.instrumentation_tests.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.instrumentation_tests.utils.routes.MockRoute
import com.mapbox.navigation.instrumentation_tests.utils.routes.MockRoutesProvider
import com.mapbox.navigation.instrumentation_tests.utils.runOnMainSync
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.ui.NavigationViewOptions
import com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions
import kotlinx.android.synthetic.main.activity_basic_navigation_view.*
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class BannerInstructionsTest :
    BaseTest<BasicNavigationViewActivity>(BasicNavigationViewActivity::class.java) {

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)

    private lateinit var mockRoute: MockRoute

    private lateinit var mapboxNavigation: MapboxNavigation

    @Before
    fun setup() {
        NavigationViewInitIdlingResource(activity.navigationView).register()
        Espresso.onIdle()

        mockRoute = MockRoutesProvider.dc_very_short(activity)
        mockWebServerRule.requestHandlers.addAll(mockRoute.mockRequestHandlers)

        val route = mockRoute.routeResponse.routes()[0]

        runOnMainSync {
            mockLocationUpdatesRule.pushLocationUpdate {
                latitude = mockRoute.routeWaypoints.first().latitude()
                longitude = mockRoute.routeWaypoints.first().longitude()
            }
            mockLocationReplayerRule.playRoute(route)
            activity.navigationView.startNavigation(
                NavigationViewOptions.builder(activity)
                    .directionsRoute(route)
                    .voiceInstructionLoaderBaseUrl(mockWebServerRule.baseUrl)
                    .build()
            )
            mapboxNavigation = activity.navigationView.retrieveMapboxNavigation()!!
        }
    }

    @Test
    fun banner_text_distance_displayed_updated() {
        mockRoute.bannerInstructions.forEach {
            val distanceView = activity.navigationView.findViewById<TextView>(R.id.stepDistanceText)
            val formatter = MapboxDistanceFormatter.Builder(activity).build()
            val routeProgressObserver = object : RouteProgressObserver {
                override fun onRouteProgressChanged(routeProgress: RouteProgress) {
                    val text = formatter.formatDistance(
                        routeProgress
                            .currentLegProgress!!
                            .currentStepProgress!!
                            .distanceRemaining
                            .toDouble()
                    ).toString()
                    Assert.assertEquals(text, (distanceView.text as SpannedString).toString())
                }
            }
            mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)

            val bannerInstructionsIdlingResource = BannerInstructionsIdlingResource(
                mapboxNavigation,
                it
            )
            bannerInstructionsIdlingResource.register()
            BaristaVisibilityAssertions.assertContains(R.id.stepPrimaryText, it.primary().text())

            it.secondary()?.run {
                BaristaVisibilityAssertions.assertContains(R.id.stepSecondaryText, this.text())
            }?.run { BaristaVisibilityAssertions.assertNotDisplayed(R.id.stepSecondaryText) }

            it.sub()?.run {
                BaristaVisibilityAssertions.assertContains(R.id.subStepText, this.text())
            }?.run { BaristaVisibilityAssertions.assertNotDisplayed(R.id.subStepLayout) }

            mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
            bannerInstructionsIdlingResource.unregister()
        }
    }
}
