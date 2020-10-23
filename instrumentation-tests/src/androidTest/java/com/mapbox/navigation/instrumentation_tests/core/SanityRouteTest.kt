package com.mapbox.navigation.instrumentation_tests.core

import androidx.test.espresso.Espresso
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.internal.extensions.applyDefaultParams
import com.mapbox.navigation.base.internal.extensions.coordinates
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.instrumentation_tests.utils.MapboxNavigationRule
import com.mapbox.navigation.instrumentation_tests.utils.Utils
import com.mapbox.navigation.instrumentation_tests.utils.assertions.RouteProgressStateTransitionAssertion
import com.mapbox.navigation.instrumentation_tests.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.idling.RouteProgressStateIdlingResource
import com.mapbox.navigation.instrumentation_tests.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.instrumentation_tests.utils.readRawFileText
import com.mapbox.navigation.instrumentation_tests.utils.route.routesRequestCallback
import com.mapbox.navigation.testing.ui.BaseTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SanityRouteTest : BaseTest<EmptyTestActivity>(EmptyTestActivity::class.java) {

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)

    private lateinit var mapboxNavigation: MapboxNavigation

    private lateinit var routeCompleteIdlingResource: RouteProgressStateIdlingResource

    @Before
    fun setup() {
        Espresso.onIdle()

        val options = MapboxNavigation.defaultNavigationOptionsBuilder(
            activity,
            Utils.getMapboxAccessToken(activity)!!
        ).build()
        mapboxNavigation = MapboxNavigationProvider.create(options)
        routeCompleteIdlingResource = RouteProgressStateIdlingResource(
            mapboxNavigation,
            RouteProgressState.ROUTE_COMPLETE
        )
    }

    @Test
    fun route_completes() {
        // prepare
        routeCompleteIdlingResource.register()
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                profile = "driving",
                jsonResponse = readRawFileText(activity, R.raw.route_response_dc_very_short),
                expectedCoordinates = listOf(
                    Point.fromLngLat(-77.031991, 38.894721),
                    Point.fromLngLat(-77.030923, 38.895433)
                )
            )
        )
        val expectedStates = RouteProgressStateTransitionAssertion(mapboxNavigation) {
            optionalState(RouteProgressState.ROUTE_INVALID)
            requiredState(RouteProgressState.LOCATION_TRACKING)
            requiredState(RouteProgressState.ROUTE_COMPLETE)
        }

        // execute
        uiDevice.run {
            mockLocationUpdatesRule.pushLocationUpdate {
                latitude = 38.894721
                longitude = -77.031991
            }
            mapboxNavigation.startTripSession()
            mapboxNavigation.requestRoutes(
                RouteOptions.builder().applyDefaultParams()
                    .baseUrl(mockWebServerRule.baseUrl)
                    .accessToken(Utils.getMapboxAccessToken(activity)!!)
                    .coordinates(
                        origin = Point.fromLngLat(-77.031991, 38.894721),
                        destination = Point.fromLngLat(-77.030923, 38.895433)
                    ).build(),
                routesRequestCallback(
                    onRoutesReady = { mockLocationReplayerRule.playRoute(it[0]) }
                )
            )
        }

        // assert and clean up
        Espresso.onIdle()
        expectedStates.assert()
        routeCompleteIdlingResource.unregister()
    }
}