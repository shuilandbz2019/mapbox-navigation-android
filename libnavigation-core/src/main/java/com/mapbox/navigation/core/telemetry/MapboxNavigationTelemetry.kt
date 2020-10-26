package com.mapbox.navigation.core.telemetry

import android.app.Application
import android.content.Context
import android.location.Location
import android.os.Build
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.telemetry.AppUserTurnstile
import com.mapbox.android.telemetry.TelemetryUtils.generateCreateDateFormatted
import com.mapbox.android.telemetry.TelemetryUtils.obtainUniversalUniqueIdentifier
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.base.common.logger.Logger
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.navigation.base.metrics.MetricEvent
import com.mapbox.navigation.base.metrics.MetricsReporter
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState.ROUTE_COMPLETE
import com.mapbox.navigation.core.BuildConfig
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.NavigationSession
import com.mapbox.navigation.core.NavigationSession.State.ACTIVE_GUIDANCE
import com.mapbox.navigation.core.NavigationSession.State.FREE_DRIVE
import com.mapbox.navigation.core.NavigationSession.State.IDLE
import com.mapbox.navigation.core.NavigationSessionStateObserver
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.internal.accounts.MapboxNavigationAccounts
import com.mapbox.navigation.core.telemetry.events.AppMetadata
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent
import com.mapbox.navigation.core.telemetry.events.MetricsRouteProgress
import com.mapbox.navigation.core.telemetry.events.NavigationArriveEvent
import com.mapbox.navigation.core.telemetry.events.NavigationCancelEvent
import com.mapbox.navigation.core.telemetry.events.NavigationDepartEvent
import com.mapbox.navigation.core.telemetry.events.NavigationEvent
import com.mapbox.navigation.core.telemetry.events.NavigationFeedbackEvent
import com.mapbox.navigation.core.telemetry.events.NavigationRerouteEvent
import com.mapbox.navigation.core.telemetry.events.PhoneState
import com.mapbox.navigation.core.telemetry.events.TelemetryLocation
import com.mapbox.navigation.core.trip.session.OffRouteObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.metrics.MapboxMetricsReporter
import com.mapbox.navigation.metrics.internal.event.NavigationAppUserTurnstileEvent
import com.mapbox.navigation.utils.internal.Time
import com.mapbox.navigation.utils.internal.ifNonNull
import java.util.Date

private data class DynamicSessionValues(
    var rerouteCount: Int = 0,
    var timeOfReroute: Long = 0L,
    var timeSinceLastReroute: Int = 0,
    var sessionId: String? = null,
    var tripIdentifier: String? = null,
    var sessionStartTime: Date? = null,
    var sessionArrivalTime: Date? = null,
    var sessionStarted: Boolean = false,
    var handleArrive: Boolean = false
) {
    fun reset() {
        rerouteCount = 0
        timeOfReroute = 0
        timeSinceLastReroute = 0
        sessionId = null
        tripIdentifier = null
        sessionStartTime = null
        sessionArrivalTime = null
        sessionStarted = false
        handleArrive = false
    }
}

/**
 * The one and only Telemetry class. This class handles all telemetry events.
 * Event List:
- appUserTurnstile
- navigation.depart
- navigation.feedback
- navigation.reroute
- navigation.arrive
- navigation.cancel
The class must be initialized before any telemetry events are reported. Attempting to use telemetry before initialization is called will throw an exception. Initialization may be called multiple times, the call is idempotent.
The class has two public methods, postUserFeedback() and initialize().
 */
internal object MapboxNavigationTelemetry :
    RouteProgressObserver,
    RoutesObserver,
    OffRouteObserver,
    NavigationSessionStateObserver {
    private const val ONE_SECOND = 1000
    private const val MOCK_PROVIDER = "com.mapbox.navigation.core.replay.ReplayLocationEngine"
    private const val EVENT_VERSION = 7
    internal val TAG = Tag("MAPBOX_TELEMETRY")

    private lateinit var context: Context // Must be context.getApplicationContext
    private lateinit var metricsReporter: MetricsReporter
    private lateinit var navigationOptions: NavigationOptions
    private var lifecycleMonitor: ApplicationLifecycleMonitor? = null
    private var appInstance: Application? = null
        set(value) {
            // Don't set it multiple times to the same value, it will cause multiple registration calls.
            if (field == value) {
                return
            }
            field = value
            ifNonNull(value) { app ->
                logger?.d(TAG, Message("Lifecycle monitor created"))
                lifecycleMonitor = ApplicationLifecycleMonitor(app)
            }
        }
    private val dynamicValues = DynamicSessionValues()
    private var locationEngineNameExternal: String = LocationEngine::javaClass.name
    private lateinit var locationsCollector: LocationsCollector
    private lateinit var sdkIdentifier: String
    private var logger: Logger? = null

    private var needHandleReroute = false
    private var sessionState: NavigationSession.State = IDLE
    private var routeProgress: RouteProgress? = null
    private var originalRoute: DirectionsRoute? = null

    /**
     * This method must be called before using the Telemetry object
     */
    fun initialize(
        mapboxNavigation: MapboxNavigation,
        options: NavigationOptions,
        reporter: MetricsReporter,
        logger: Logger?
    ) {
        this.logger = logger
        this.locationsCollector = LocationsCollectorImpl(logger)
        navigationOptions = options
        context = options.applicationContext
        locationEngineNameExternal = options.locationEngine.javaClass.name
        sdkIdentifier = if (options.isFromNavigationUi) {
            "mapbox-navigation-ui-android"
        } else {
            "mapbox-navigation-android"
        }
        metricsReporter = reporter

        registerListeners(mapboxNavigation)
        postTurnstileEvent()
        this.logger?.d(TAG, Message("Valid initialization"))
    }

    private fun sessionStart() {
        logger?.d(TAG, Message("sessionStart"))
        locationsCollector.flushBuffers()
        resetRouteProgress()
        originalRoute?.let { route ->
            dynamicValues.run {
                sessionId = obtainUniversalUniqueIdentifier()
                sessionStartTime = Date()
                sessionStarted = true
                handleArrive = true
            }

            val departureEvent = NavigationDepartEvent(PhoneState(context))
            populateNavigationEvent(departureEvent, route)
            sendMetricEvent(departureEvent)
        }
    }

    private fun sessionStop() {
        if (dynamicValues.sessionStarted) {
            logger?.d(TAG, Message("sessionStop"))
            handleSessionCanceled()
            dynamicValues.reset()
        }
    }

    private fun sendMetricEvent(event: MetricEvent) {
        if (isTelemetryAvailable()) {
            logger?.d(TAG, Message("${event::class.java} event sent"))
            metricsReporter.addEvent(event)
        } else {
            logger?.d(
                TAG,
                Message(
                    "${event::class.java} not sent. Caused by: " +
                        "Navigation Session started: ${dynamicValues.sessionStarted}. " +
                        "Route exists: ${originalRoute != null}"
                )
            )
        }
    }

    /**
     * The Navigation session is considered to be guided if it has been started and at least one route is active,
     * it is a free drive / idle session otherwise
     */
    private fun isTelemetryAvailable(): Boolean {
        return originalRoute != null && dynamicValues.sessionStarted
    }

    fun setApplicationInstance(app: Application) {
        appInstance = app
    }

    private fun handleReroute(newRoute: DirectionsRoute) {
        logger?.d(TAG, Message("handleReroute"))
        dynamicValues.run {
            val currentTime = Time.SystemImpl.millis()
            timeSinceLastReroute = (currentTime - timeOfReroute).toInt()
            timeOfReroute = currentTime
            rerouteCount++
        }

        val navigationRerouteEvent = NavigationRerouteEvent(
            PhoneState(context),
            MetricsRouteProgress(routeProgress)
        ).apply {
            secondsSinceLastReroute = dynamicValues.timeSinceLastReroute / ONE_SECOND
            newDistanceRemaining = newRoute.distance().toInt()
            newDurationRemaining = newRoute.duration().toInt()
            newGeometry = obtainGeometry(newRoute)
        }
        populateNavigationEvent(navigationRerouteEvent)

        locationsCollector.collectLocations { preEventBuffer, postEventBuffer ->
            navigationRerouteEvent.apply {
                locationsBefore = preEventBuffer.toTelemetryLocations()
                locationsAfter = postEventBuffer.toTelemetryLocations()
            }

            sendMetricEvent(navigationRerouteEvent)
        }
    }

    private fun handleExternalRoute(route: DirectionsRoute) {
        logger?.d(TAG, Message("handleExternalRoute"))
        sessionStop()
        resetOriginalRoute(route)
        sessionStart()
    }

    fun postUserFeedback(
        @FeedbackEvent.Type feedbackType: String,
        description: String,
        @FeedbackEvent.Source feedbackSource: String,
        screenshot: String?,
        feedbackSubType: Array<String>?,
        appMetadata: AppMetadata?
    ) {
        if (dynamicValues.sessionStarted) {
            logger?.d(TAG, Message("collect post event locations for user feedback"))
            val feedbackEvent = NavigationFeedbackEvent(
                PhoneState(context),
                MetricsRouteProgress(routeProgress)
            ).apply {
                this.feedbackType = feedbackType
                this.source = feedbackSource
                this.description = description
                this.screenshot = screenshot
                this.feedbackSubType = feedbackSubType
                this.appMetadata = appMetadata
            }
            populateNavigationEvent(feedbackEvent)

            locationsCollector.collectLocations { preEventBuffer, postEventBuffer ->
                logger?.d(TAG, Message("locations ready"))
                feedbackEvent.apply {
                    locationsBefore = preEventBuffer.toTelemetryLocations()
                    locationsAfter = postEventBuffer.toTelemetryLocations()
                }
                sendMetricEvent(feedbackEvent)
            }
        }
    }

    private fun handleSessionCanceled() {
        logger?.d(TAG, Message("handleSessionCanceled"))
        locationsCollector.flushBuffers()

        val cancelEvent = NavigationCancelEvent(PhoneState(context))
        ifNonNull(dynamicValues.sessionArrivalTime) {
            cancelEvent.arrivalTimestamp = generateCreateDateFormatted(it)
        }
        populateNavigationEvent(cancelEvent)
        sendMetricEvent(cancelEvent)
    }

    private fun postTurnstileEvent() {
        val turnstileEvent =
            AppUserTurnstile(sdkIdentifier, BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME).also {
                it.setSkuId(MapboxNavigationAccounts.getInstance(context).obtainSkuId())
            }
        val event = NavigationAppUserTurnstileEvent(turnstileEvent)
        metricsReporter.addEvent(event)
    }

    private fun processArrival() {
        if (dynamicValues.sessionStarted && dynamicValues.handleArrive) {
            logger?.d(TAG, Message("you have arrived"))

            dynamicValues.run {
                tripIdentifier = obtainUniversalUniqueIdentifier()
                sessionArrivalTime = Date()
                handleArrive = false
            }

            val arriveEvent = NavigationArriveEvent(PhoneState(context))
            populateNavigationEvent(arriveEvent)
            sendMetricEvent(arriveEvent)
        }
    }

    private fun registerListeners(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.run {
            registerLocationObserver(locationsCollector)
            registerRouteProgressObserver(this@MapboxNavigationTelemetry)
            registerRoutesObserver(this@MapboxNavigationTelemetry)
            registerOffRouteObserver(this@MapboxNavigationTelemetry)
            registerNavigationSessionObserver(this@MapboxNavigationTelemetry)
        }
    }

    fun unregisterListeners(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.run {
            unregisterLocationObserver(locationsCollector)
            unregisterRouteProgressObserver(this@MapboxNavigationTelemetry)
            unregisterRoutesObserver(this@MapboxNavigationTelemetry)
            unregisterOffRouteObserver(this@MapboxNavigationTelemetry)
            unregisterNavigationSessionObserver(this@MapboxNavigationTelemetry)
        }
        MapboxMetricsReporter.disable()
    }

    private fun populateNavigationEvent(
        navigationEvent: NavigationEvent,
        route: DirectionsRoute? = null
    ) {
        logger?.d(TAG, Message("populateNavigationEvent"))

        val directionsRoute = route ?: routeProgress?.route

        navigationEvent.apply {
            sdkIdentifier = this@MapboxNavigationTelemetry.sdkIdentifier

            routeProgress?.let { routeProgress ->
                stepIndex = routeProgress.currentLegProgress?.currentStepProgress?.stepIndex ?: 0

                distanceRemaining = routeProgress.distanceRemaining.toInt()
                durationRemaining = routeProgress.durationRemaining.toInt()
                distanceCompleted = routeProgress.distanceTraveled.toInt()

                routeProgress.route.let {
                    geometry = it.geometry()
                    profile = it.routeOptions()?.profile()
                    requestIdentifier = it.routeOptions()?.requestUuid()
                    stepCount = obtainStepCount(it)
                    legIndex = it.routeIndex()?.toInt() ?: 0
                    legCount = it.legs()?.size ?: 0
                }
            }

            originalRoute?.let {
                originalStepCount = obtainStepCount(it)
                originalEstimatedDistance = it.distance().toInt()
                originalEstimatedDuration = it.duration().toInt()
                originalRequestIdentifier = it.routeOptions()?.requestUuid()
                originalGeometry = it.geometry()
            }

            locationEngine = locationEngineNameExternal
            tripIdentifier = obtainUniversalUniqueIdentifier()
            lat = locationsCollector.lastLocation?.latitude ?: 0.0
            lng = locationsCollector.lastLocation?.longitude ?: 0.0
            simulation = locationEngineNameExternal == MOCK_PROVIDER
            percentTimeInPortrait = lifecycleMonitor?.obtainPortraitPercentage() ?: 100
            percentTimeInForeground = lifecycleMonitor?.obtainForegroundPercentage() ?: 100

            dynamicValues.let {
                startTimestamp = generateCreateDateFormatted(it.sessionStartTime)
                rerouteCount = it.rerouteCount
                sessionIdentifier = it.sessionId
            }

            eventVersion = EVENT_VERSION

            directionsRoute?.let {
                absoluteDistanceToDestination = obtainAbsoluteDistance(
                    locationsCollector.lastLocation,
                    obtainRouteDestination(it)
                )
                estimatedDistance = it.distance().toInt()
                estimatedDuration = it.duration().toInt()
                totalStepCount = obtainStepCount(it)
            }
        }
    }

    override fun onRouteProgressChanged(routeProgress: RouteProgress) {
        this.routeProgress = routeProgress
        if (routeProgress.currentState == ROUTE_COMPLETE) {
            processArrival()
        }
    }

    private fun resetRouteProgress() {
        routeProgress = null
    }

    private fun resetOriginalRoute(route: DirectionsRoute? = null) {
        logger?.d(TAG, Message("resetOriginalRoute"))
        originalRoute = route
    }

    private var needHandleSessionStart = false

    override fun onRoutesChanged(routes: List<DirectionsRoute>) {
        logger?.d(TAG, Message("onRoutesChanged received. Route list size = ${routes.size}"))
        routes.getOrNull(0)?.let {
            if (sessionState == ACTIVE_GUIDANCE) {
                if (originalRoute != null) {
                    if (needHandleReroute) {
                        needHandleReroute = false
                        handleReroute(it)
                    } else {
                        handleExternalRoute(it)
                    }
                } else {
                    originalRoute = it
                    if (needHandleSessionStart) {
                        needHandleSessionStart = false
                        sessionStart()
                    }
                }
            } else {
                originalRoute = it
            }
        }
    }

    override fun onOffRouteStateChanged(offRoute: Boolean) {
        logger?.d(TAG, Message("onOffRouteStateChanged $offRoute"))
        if (offRoute) {
            needHandleReroute = true
        }
    }

    override fun onNavigationSessionStateChanged(navigationSession: NavigationSession.State) {
        logger?.d(TAG, Message("session state is $navigationSession"))
        sessionState = navigationSession
        when (navigationSession) {
            IDLE, FREE_DRIVE -> {
                sessionStop()
                resetOriginalRoute()
            }
            ACTIVE_GUIDANCE -> {
                if (originalRoute != null) {
                    sessionStart()
                } else {
                    needHandleSessionStart = true
                }
            }
        }
    }

    private fun List<Location>.toTelemetryLocations(): Array<TelemetryLocation> {
        val feedbackLocations = mutableListOf<TelemetryLocation>()
        this.forEach {
            feedbackLocations.add(
                TelemetryLocation(
                    it.latitude,
                    it.longitude,
                    it.speed,
                    it.bearing,
                    it.altitude,
                    it.time.toString(),
                    it.accuracy,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        it.verticalAccuracyMeters
                    } else {
                        0f
                    }
                )
            )
        }

        return feedbackLocations.toTypedArray()
    }
}
