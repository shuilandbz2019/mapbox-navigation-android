package com.mapbox.navigation.instrumentation_tests.utils.routes

import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.geojson.Point
import com.mapbox.navigation.testing.ui.http.MockRequestHandler

data class MockRoute(
    val routeResponseJson: String,
    val routeResponse: DirectionsResponse,
    val mockRequestHandlers: List<MockRequestHandler>,
    val routeWaypoints: List<Point>,
    val bannerInstructions: List<BannerInstructions>
)
