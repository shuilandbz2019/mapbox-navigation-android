package com.mapbox.navigation.testing.ui.http

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class MockWebServerRule : TestWatcher() {

    val webServer = MockWebServer()
    val baseUrl = webServer.url("").toString()

    /**
     * Cleared after each test.
     */
    val requestHandlers = mutableListOf<MockRequestHandler>()

    init {
        webServer.setDispatcher(object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requestHandlers.forEach {
                    it.handle(request)?.run { return this }
                }
                throw IllegalStateException(
                    """request url:
                      |${request.path}
                      |is not handled
                    """.trimMargin()
                )
            }
        })
    }

    override fun starting(description: Description?) {
        // no need to start the webServer, already started implicitly when fetching baseUrl
        // webServer.start()
    }

    override fun finished(description: Description?) {
        requestHandlers.clear()
        webServer.shutdown()
    }
}

/*val handshaCertificate = HandshakeCertificates.Builder()
            .addPlatformTrustedCertificates()
            .addTrustedCertificate()
        val localhostCertificate = HeldCertificate.Builder()
            .addSubjectAlternativeName(InetAddress.getByName("localhost").canonicalHostName)
            .build()*/
// webServer.useHttps(SSLContext.getDefault().socketFactory, false)
// webServer.useHttps(TlsUtil.localhost().sslSocketFactory(), false)
