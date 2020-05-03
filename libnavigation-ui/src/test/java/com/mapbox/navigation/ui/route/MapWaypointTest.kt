package com.mapbox.navigation.ui.route

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.maps.Style
import io.mockk.mockk
import org.junit.Assert
import org.junit.Test

class MapWaypointTest {

    private val style: Style = mockk()

    private val mapWaypoints = MapWaypoints(style)

    @Test
    fun buildWayPointFeatureCollection() {
        val route = getDirectionsRoute()

        mapWaypoints.drawWayPoints(route)

        Assert.assertEquals(2, mapWaypoints.waypointSource.querySourceFeatures(null).size)
    }

    @Test
    fun buildWayPointFeatureCollectionFirstFeatureOrigin() {
        val route = getDirectionsRoute()

        mapWaypoints.drawWayPoints(route)

        val features = mapWaypoints.waypointSource.querySourceFeatures(null)
        Assert.assertEquals("{\"wayPoint\":\"origin\"}", features[0].properties().toString())
    }

    @Test
    fun buildWayPointFeatureCollectionSecondFeatureOrigin() {
        val route = getDirectionsRoute()

        mapWaypoints.drawWayPoints(route)

        val features = mapWaypoints.waypointSource.querySourceFeatures(null)
        Assert.assertEquals("{\"wayPoint\":\"destination\"}",features[1].properties().toString())
    }

    @Test
    fun buildWayPointFeatureFromLeg() {
        val route = getDirectionsRoute()

        mapWaypoints.drawWayPoints(route)

        val features = mapWaypoints.waypointSource.querySourceFeatures(null)
        Assert.assertEquals(-122.523514, (features[0].geometry() as Point).coordinates()[0], 0.0)
        Assert.assertEquals(37.975355, (features[0].geometry() as Point).coordinates()[1], 0.0)
    }

    @Test
    fun buildWayPointFeatureFromLegContainsOriginWaypoint() {
        val route = getDirectionsRoute()

        mapWaypoints.drawWayPoints(route)

        val features = mapWaypoints.waypointSource.querySourceFeatures(null)
        Assert.assertEquals("\"origin\"", features[0].properties()!!["wayPoint"].toString())
    }

    private fun getDirectionsRoute(): DirectionsRoute {
        val tokenHere = "someToken"
        val directionsRouteAsJson = "{\"routeIndex\":\"0\",\"distance\":66.9,\"duration\":45.0,\"geometry\":\"urylgArvfuhFjJ`CbC{[pAZ\",\"weight\":96.6,\"weight_name\":\"routability\",\"legs\":[{\"distance\":66.9,\"duration\":45.0,\"summary\":\"Laurel Place, Lincoln Avenue\",\"steps\":[{\"distance\":21.0,\"duration\":16.7,\"geometry\":\"urylgArvfuhFjJ`C\",\"name\":\"\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.523514,37.975355],\"bearing_before\":0.0,\"bearing_after\":196.0,\"instruction\":\"Head south\",\"type\":\"depart\",\"modifier\":\"right\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":21.0,\"announcement\":\"Head south, then turn left onto Laurel Place\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eHead south, then turn left onto Laurel Place\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"},{\"distanceAlongGeometry\":18.9,\"announcement\":\"Turn left onto Laurel Place, then turn right onto Lincoln Avenue\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eTurn left onto Laurel Place, then turn right onto Lincoln Avenue\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":21.0,\"primary\":{\"text\":\"Laurel Place\",\"components\":[{\"text\":\"Laurel Place\",\"type\":\"text\",\"abbr\":\"Laurel Pl\",\"abbr_priority\":0}],\"type\":\"turn\",\"modifier\":\"left\"}},{\"distanceAlongGeometry\":18.9,\"primary\":{\"text\":\"Laurel Place\",\"components\":[{\"text\":\"Laurel Place\",\"type\":\"text\",\"abbr\":\"Laurel Pl\",\"abbr_priority\":0}],\"type\":\"turn\",\"modifier\":\"left\"},\"sub\":{\"text\":\"Lincoln Avenue\",\"components\":[{\"text\":\"Lincoln Avenue\",\"type\":\"text\",\"abbr\":\"Lincoln Ave\",\"abbr_priority\":0}],\"type\":\"turn\",\"modifier\":\"right\"}}],\"driving_side\":\"right\",\"weight\":52.6,\"intersections\":[{\"location\":[-122.523514,37.975355],\"bearings\":[196],\"entry\":[true],\"out\":0}]},{\"distance\":41.2,\"duration\":27.3,\"geometry\":\"igylgAtzfuhFbC{[\",\"name\":\"Laurel Place\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.523579,37.975173],\"bearing_before\":195.0,\"bearing_after\":99.0,\"instruction\":\"Turn left onto Laurel Place\",\"type\":\"turn\",\"modifier\":\"left\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":22.6,\"announcement\":\"Turn right onto Lincoln Avenue, then you will arrive at your destination\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eTurn right onto Lincoln Avenue, then you will arrive at your destination\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":41.2,\"primary\":{\"text\":\"Lincoln Avenue\",\"components\":[{\"text\":\"Lincoln Avenue\",\"type\":\"text\",\"abbr\":\"Lincoln Ave\",\"abbr_priority\":0}],\"type\":\"turn\",\"modifier\":\"right\"}}],\"driving_side\":\"right\",\"weight\":43.0,\"intersections\":[{\"location\":[-122.523579,37.975173],\"bearings\":[15,105,285],\"entry\":[false,true,true],\"in\":0,\"out\":1}]},{\"distance\":4.7,\"duration\":1.0,\"geometry\":\"ecylgAx}euhFpAZ\",\"name\":\"Lincoln Avenue\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.523117,37.975107],\"bearing_before\":99.0,\"bearing_after\":194.0,\"instruction\":\"Turn right onto Lincoln Avenue\",\"type\":\"turn\",\"modifier\":\"right\"},\"voiceInstructions\":[{\"distanceAlongGeometry\":4.7,\"announcement\":\"You have arrived at your destination\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eYou have arrived at your destination\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}],\"bannerInstructions\":[{\"distanceAlongGeometry\":4.7,\"primary\":{\"text\":\"You have arrived\",\"components\":[{\"text\":\"You have arrived\",\"type\":\"text\"}],\"type\":\"arrive\",\"modifier\":\"straight\"}}],\"driving_side\":\"right\",\"weight\":1.0,\"intersections\":[{\"location\":[-122.523117,37.975107],\"bearings\":[15,105,195,285],\"entry\":[true,true,true,false],\"in\":3,\"out\":2}]},{\"distance\":0.0,\"duration\":0.0,\"geometry\":\"s`ylgAt~euhF\",\"name\":\"Lincoln Avenue\",\"mode\":\"driving\",\"maneuver\":{\"location\":[-122.523131,37.975066],\"bearing_before\":195.0,\"bearing_after\":0.0,\"instruction\":\"You have arrived at your destination\",\"type\":\"arrive\"},\"voiceInstructions\":[],\"bannerInstructions\":[],\"driving_side\":\"right\",\"weight\":0.0,\"intersections\":[{\"location\":[-122.523131,37.975066],\"bearings\":[15],\"entry\":[true],\"in\":0}]}],\"annotation\":{\"distance\":[21.030105037432428,41.16669115760234,4.722589365163041],\"congestion\":[]}}],\"routeOptions\":{\"baseUrl\":\"https://api.mapbox.com\",\"user\":\"mapbox\",\"profile\":\"driving-traffic\",\"coordinates\":[[-122.5237559,37.9754094],[-122.5231475,37.9750697]],\"alternatives\":true,\"language\":\"en\",\"continue_straight\":false,\"roundabout_exits\":false,\"geometries\":\"polyline6\",\"overview\":\"full\",\"steps\":true,\"annotations\":\"congestion,distance\",\"voice_instructions\":true,\"banner_instructions\":true,\"voice_units\":\"imperial\",\"access_token\":\"$tokenHere\",\"uuid\":\"ck9g2sbdk6pod7ynuece0r2yo\"},\"voiceLocale\":\"en-US\"}"
        return DirectionsRoute.fromJson(directionsRouteAsJson)
    }
}