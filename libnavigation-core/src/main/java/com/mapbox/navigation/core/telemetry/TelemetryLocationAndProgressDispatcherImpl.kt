package com.mapbox.navigation.core.telemetry

import android.location.Location
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.base.common.logger.Logger
import com.mapbox.base.common.logger.model.Message
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.NavigationSession.State
import com.mapbox.navigation.core.NavigationSession.State.ACTIVE_GUIDANCE
import com.mapbox.navigation.core.NavigationSession.State.FREE_DRIVE
import com.mapbox.navigation.core.NavigationSession.State.IDLE
import com.mapbox.navigation.core.telemetry.MapboxNavigationTelemetry.TAG
import com.mapbox.navigation.core.telemetry.NewRoute.ExternalRoute
import com.mapbox.navigation.core.telemetry.NewRoute.RerouteRoute
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import java.util.Collections.synchronizedList

internal class TelemetryLocationAndProgressDispatcherImpl(
    private val logger: Logger?
) :
    TelemetryLocationAndProgressDispatcher {

    companion object {
        private const val LOCATION_BUFFER_MAX_SIZE = 20
    }

    private val locationsBuffer = synchronizedList(mutableListOf<Location>())
    private val eventsLocationsBuffer = synchronizedList(mutableListOf<EventLocations>())

    override val routeProgressChannel = Channel<RouteProgress>(Channel.CONFLATED)
    override val newRouteChannel = Channel<NewRoute>(Channel.CONFLATED)
    override val sessionStateChannel = Channel<State>(Channel.CONFLATED)

    override val lastLocation: Location?
        get() = locationsBuffer.lastOrNull()
    override var routeProgress: RouteProgress? = null
    override var originalRoute = CompletableDeferred<DirectionsRoute>()
    private var needHandleReroute = false
    private var sessionState: State = IDLE

    private fun accumulatePostEventLocation(location: Location) {
        val iterator = eventsLocationsBuffer.iterator()
        while (iterator.hasNext()) {
            iterator.next().let {
                it.addPostEventLocation(location)
                if (it.postEventLocationsSize() >= LOCATION_BUFFER_MAX_SIZE) {
                    it.onBufferFull()
                    iterator.remove()
                }
            }
        }
    }

    private fun flushLocationEventBuffer() {
        logger?.d(
            TAG,
            Message(
                "flushing eventsLocationsBuffer. Pending events = ${eventsLocationsBuffer.size}"
            )
        )
        eventsLocationsBuffer.forEach { it.onBufferFull() }
    }

    private fun accumulateLocation(location: Location) {
        locationsBuffer.run {
            if (size >= LOCATION_BUFFER_MAX_SIZE) {
                removeAt(0)
            }
            add(location)
        }
    }

    override fun accumulatePostEventLocations(
        onBufferFull: (List<Location>, List<Location>) -> Unit
    ) {
        eventsLocationsBuffer.add(
            EventLocations(
                locationsBuffer.getCopy(),
                mutableListOf(),
                onBufferFull
            )
        )
    }

    override fun clearLocationEventBuffer() {
        flushLocationEventBuffer()
        eventsLocationsBuffer.clear()
    }

    override fun onRouteProgressChanged(routeProgress: RouteProgress) {
        this.routeProgress = routeProgress
        routeProgressChannel.offer(routeProgress)
    }

    override fun resetOriginalRoute(route: DirectionsRoute?) {
        logger?.d(TAG, Message("resetOriginalRoute"))
        originalRoute = if (route != null) {
            CompletableDeferred(route)
        } else {
            CompletableDeferred()
        }
    }

    override fun resetRouteProgress() {
        routeProgress = null
        routeProgressChannel.poll() // remove element from the channel if exists
    }

    override fun onRawLocationChanged(rawLocation: Location) {
        accumulateLocation(rawLocation)
        accumulatePostEventLocation(rawLocation)
    }

    override fun onEnhancedLocationChanged(enhancedLocation: Location, keyPoints: List<Location>) {
        // Do nothing
    }

    override fun onRoutesChanged(routes: List<DirectionsRoute>) {
        logger?.d(TAG, Message("onRoutesChanged received. Route list size = ${routes.size}"))
        routes.getOrNull(0)?.let {
            if (sessionState == ACTIVE_GUIDANCE) {
                if (originalRoute.isCompleted) {
                    newRouteChannel.offer(
                        if (needHandleReroute) {
                            needHandleReroute = false
                            RerouteRoute(it)
                        } else {
                            ExternalRoute(it)
                        }
                    )
                }
                originalRoute.complete(it)
            } else {
                originalRoute = CompletableDeferred(it)
            }
        }
    }

    override fun onOffRouteStateChanged(offRoute: Boolean) {
        logger?.d(TAG, Message("onOffRouteStateChanged $offRoute"))
        if (offRoute) {
            needHandleReroute = true
        }
    }

    override fun onNavigationSessionStateChanged(navigationSession: State) {
        logger?.d(TAG, Message("session state is $navigationSession"))
        sessionState = navigationSession
        sessionStateChannel.offer(navigationSession)
    }

    @Synchronized
    private fun <T> MutableList<T>.getCopy(): List<T> {
        return mutableListOf<T>().also {
            it.addAll(this)
        }
    }
}
