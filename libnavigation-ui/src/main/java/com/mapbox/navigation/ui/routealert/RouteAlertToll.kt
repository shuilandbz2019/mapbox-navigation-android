package com.mapbox.navigation.ui.routealert

import androidx.core.content.ContextCompat
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.mapboxsdk.style.expressions.Expression
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.navigation.base.trip.model.alert.RouteAlert
import com.mapbox.navigation.base.trip.model.alert.TollCollectionAlert
import com.mapbox.navigation.base.trip.model.alert.TollCollectionType
import com.mapbox.navigation.ui.R
import com.mapbox.navigation.ui.routealert.MapboxRouteAlert.Companion.getMapboxRouteAlertSymbolLayerProperties

/**
 * Handle [TollCollectionAlert] to add an icon on the map to indicate where a toll is presented.
 *
 * @param routeAlertViewOptions the options to build a route alert toll view
 */

class RouteAlertToll(
    private val routeAlertViewOptions: RouteAlertViewOptions
) {
    private val tollCollectionsSource = GeoJsonSource(MAPBOX_TOLL_COLLECTIONS_SOURCE)
    private val tollCollectionsLayer = SymbolLayer(
        MAPBOX_TOLL_COLLECTIONS_LAYER,
        MAPBOX_TOLL_COLLECTIONS_SOURCE
    )
        .withProperties(
            *routeAlertViewOptions.properties.let {
                if (it.isEmpty()) {
                    getMapboxRouteAlertSymbolLayerProperties()
                } else {
                    it
                }
            },
            PropertyFactory.iconImage(MAPBOX_TOLL_COLLECTIONS_IMAGE_PROPERTY_ID),
            PropertyFactory.textField(
                Expression.get(Expression.literal(MAPBOX_TOLL_COLLECTIONS_TEXT_PROPERTY_ID))
            )
        )

    init {
        routeAlertViewOptions.style.addImage(
            MAPBOX_TOLL_COLLECTIONS_IMAGE_PROPERTY_ID,
            routeAlertViewOptions.drawable
                ?: ContextCompat.getDrawable(
                    routeAlertViewOptions.context,
                    R.drawable.mapbox_ic_route_alert_toll
                )!!
        )
        routeAlertViewOptions.style.addSource(tollCollectionsSource)
        routeAlertViewOptions.style.addLayer(tollCollectionsLayer)
    }

    /**
     * Display toll type route alerts on the map.
     * The [RouteAlertModelToll.tollDescription] is the text shown under the icon.
     *
     * @param tollAlertModels a list of toll alert models
     */
    fun onNewRouteTollAlerts(tollAlertModels: List<RouteAlertModelToll>) {
        val tollCollectionFeatures = mutableListOf<Feature>()
        tollAlertModels.forEach {
            val feature = Feature.fromGeometry(it.coordinate)
            feature.addStringProperty(
                MAPBOX_TOLL_COLLECTIONS_TEXT_PROPERTY_ID,
                it.tollDescription
            )
            tollCollectionFeatures.add(feature)
        }
        tollCollectionsSource.setGeoJson(
            FeatureCollection.fromFeatures(tollCollectionFeatures)
        )
    }

    /**
     * Display [TollCollectionAlert] on the map with the default toll description provided by Mapbox.
     *
     * @param routeAlerts a list of route alerts, it may or may not contain [TollCollectionAlert]
     */
    fun onNewRouteAlerts(routeAlerts: List<RouteAlert>) {
        routeAlerts.filterIsInstance<TollCollectionAlert>().run {
            val tollAlertModels = mutableListOf<RouteAlertModelToll>()
            forEach { tollCollectionAlert ->
                val typeString = when (tollCollectionAlert.tollCollectionType) {
                    TollCollectionType.TollGantry -> {
                        routeAlertViewOptions.context.resources
                            .getString(R.string.mapbox_route_alert_toll_gantry)
                    }
                    TollCollectionType.TollBooth -> {
                        routeAlertViewOptions.context.resources
                            .getString(R.string.mapbox_route_alert_toll_booth)
                    }
                    TollCollectionType.Unknown -> {
                        routeAlertViewOptions.context.resources
                            .getString(R.string.mapbox_route_alert_toll_general)
                    }
                    else -> {
                        return@forEach
                    }
                }

                val feature = RouteAlertModelToll(
                    tollCollectionAlert.coordinate,
                    typeString
                )
                tollAlertModels.add(feature)
            }
            onNewRouteTollAlerts(tollAlertModels)
        }
    }

    companion object {
        private const val MAPBOX_TOLL_COLLECTIONS_SOURCE = "mapbox_toll_collections_source"
        private const val MAPBOX_TOLL_COLLECTIONS_LAYER = "mapbox_toll_collections_layer"
        private const val MAPBOX_TOLL_COLLECTIONS_TEXT_PROPERTY_ID =
            "mapbox_toll_collections_text_property_id"
        private const val MAPBOX_TOLL_COLLECTIONS_IMAGE_PROPERTY_ID =
            "mapbox_toll_collections_image_property_id"
    }
}
