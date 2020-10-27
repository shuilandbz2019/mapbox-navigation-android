package com.mapbox.navigation.instrumentation_tests.utils.routes

import android.content.Context
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.geojson.Point
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.utils.bufferFromRawFile
import com.mapbox.navigation.instrumentation_tests.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.http.MockVoiceRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.readRawFileText

object MockRoutesProvider {

    fun dc_very_short(context: Context): MockRoute {
        val jsonResponse = readRawFileText(context, R.raw.route_response_dc_very_short)
        val coordinates = listOf(
            Point.fromLngLat(-77.031991, 38.894721),
            Point.fromLngLat(-77.030923, 38.895433)
        )
        return MockRoute(
            jsonResponse,
            DirectionsResponse.fromJson(jsonResponse),
            listOf(
                MockDirectionsRequestHandler(
                    profile = "driving",
                    jsonResponse = readRawFileText(context, R.raw.route_response_dc_very_short),
                    expectedCoordinates = coordinates
                ),
                MockVoiceRequestHandler(
                    buffer = bufferFromRawFile(context, R.raw.route_response_dc_very_short_voice_1),
                    requestDetails = """%3Cspeak%3E%3Camazon:effect%20name=%22drc%22%3E%3Cprosody%20rate=%221.08%22%3EDrive%20north%20on%2014th%20Street%20Northwest.%20Then%20Turn%20right%20onto%20Pennsylvania%20Avenue%20Northwest.%3C%2Fprosody%3E%3C%2"""
                ),
                MockVoiceRequestHandler(
                    buffer = bufferFromRawFile(context, R.raw.route_response_dc_very_short_voice_2),
                    requestDetails = """%3Cspeak%3E%3Camazon:effect%20name=%22drc%22%3E%3Cprosody%20rate=%221.08%22%3EYou%20have%20arrived%20at%20your%20destination.%3C%2Fprosody%3E%3C%2F"""
                )
            ),
            coordinates
        )
    }
}
