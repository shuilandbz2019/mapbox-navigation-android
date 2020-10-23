package com.mapbox.navigation.instrumentation_tests.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.ui.NavigationViewOptions
import com.mapbox.navigation.ui.OnNavigationReadyCallback
import com.mapbox.navigation.ui.listeners.BannerInstructionsListener
import com.mapbox.navigation.ui.listeners.NavigationListener
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import kotlinx.android.synthetic.main.activity_basic_navigation_view.*

class BasicNavigationViewActivity :
    AppCompatActivity(),
    OnNavigationReadyCallback,
    NavigationListener,
    BannerInstructionsListener {

    lateinit var navigationMapboxMap: NavigationMapboxMap
    lateinit var mapboxNavigation: MapboxNavigation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_basic_navigation_view)

        navigationView.onCreate(savedInstanceState)
        navigationView.initialize(this)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        navigationView.onLowMemory()
    }

    override fun onResume() {
        super.onResume()
        navigationView.onResume()
    }

    override fun onPause() {
        super.onPause()
        navigationView.onPause()
    }

    override fun onStart() {
        super.onStart()
        navigationView.onStart()
    }

    override fun onStop() {
        super.onStop()
        navigationView.onStop()
    }

    override fun onDestroy() {
        navigationView.onDestroy()
        super.onDestroy()
    }

    override fun onBackPressed() {
        // If the navigation view didn't need to do anything, call super
        if (!navigationView.onBackPressed()) {
            super.onBackPressed()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        navigationView.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        navigationView.onRestoreInstanceState(savedInstanceState)
    }

    override fun onNavigationReady(isRunning: Boolean) {
        if (!isRunning && !::navigationMapboxMap.isInitialized) {
            ifNonNull(navigationView.retrieveNavigationMapboxMap()) { navMapboxMap ->
                this.navigationMapboxMap = navMapboxMap
                this.navigationMapboxMap.updateLocationLayerRenderMode(RenderMode.NORMAL)
                navigationView.retrieveMapboxNavigation()?.let { this.mapboxNavigation = it }

                val optionsBuilder = NavigationViewOptions.builder(this)
                optionsBuilder.navigationListener(this)
                optionsBuilder.directionsRoute(route)
                optionsBuilder.shouldSimulateRoute(true)
                optionsBuilder.bannerInstructionsListener(this)
                navigationView.startNavigation(optionsBuilder.build())
            }
        }
    }

    override fun willDisplay(instructions: BannerInstructions?): BannerInstructions? {
        return instructions
    }

    override fun onNavigationRunning() {
        // todo
    }

    override fun onNavigationFinished() {
        finish()
    }

    override fun onCancelNavigation() {
        navigationView.stopNavigation()
        finish()
    }
}
