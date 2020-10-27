package com.mapbox.navigation.instrumentation_tests.utils.http

import com.mapbox.navigation.testing.ui.http.MockRequestHandler
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer

class MockVoiceRequestHandler(
    private val buffer: Buffer,
    private val requestDetails: String
) : MockRequestHandler {

    override fun handle(request: RecordedRequest): MockResponse? {
        val prefix = """/voice/v1/speak/$requestDetails"""
        return if (request.path.startsWith(prefix)) {
            MockResponse().apply { body = buffer }
        } else {
            null
        }
    }
}
