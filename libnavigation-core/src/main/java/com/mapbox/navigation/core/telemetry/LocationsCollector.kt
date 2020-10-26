package com.mapbox.navigation.core.telemetry

import android.location.Location
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.NavigationSession
import com.mapbox.navigation.core.NavigationSessionStateObserver
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.OffRouteObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.ReceiveChannel

internal interface LocationsCollector : LocationObserver {
    val lastLocation: Location?

    fun flushBuffers()
    fun collectLocations(onBufferFull: (List<Location>, List<Location>) -> Unit)
}
