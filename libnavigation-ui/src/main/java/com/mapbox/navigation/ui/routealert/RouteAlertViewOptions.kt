package com.mapbox.navigation.ui.routealert

import android.content.Context
import android.graphics.drawable.Drawable
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.PropertyValue

/**
 * The options to build a RouteAlert view, refer to [RouteAlertToll].
 *
 * @param context to retrieve drawable/string resources
 * @param style to add source/layer into
 * @param drawable the icon which represents a route alert.
 *  Use default icon if is not assigned and is necessary.
 * @param properties customized properties. Don't customize the 'iconImage' and 'textField'
 *  properties. These two properties will be dropped and use the default keys.
 */
class RouteAlertViewOptions private constructor(
    val context: Context,
    val style: Style,
    val drawable: Drawable?,
    val properties: Array<PropertyValue<out Any>>
) {

    fun toBuilder() = Builder(context, style).apply {
        drawable?.let {
            drawable(it)
        }
        properties(properties)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RouteAlertViewOptions

        if (context != other.context) return false
        if (style != other.style) return false
        if (drawable != other.drawable) return false
        if (!properties.contentEquals(other.properties)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = context.hashCode()
        result = 31 * result + style.hashCode()
        result = 31 * result + (drawable?.hashCode() ?: 0)
        result = 31 * result + properties.contentHashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RouteAlertViewOptions(" +
            "context=$context, " +
            "style=$style, " +
            "drawable=$drawable, " +
            "properties=$properties" +
            ")"
    }

    class Builder(
        private val context: Context,
        private val style: Style
    ) {
        private var drawable: Drawable? = null
        private var properties: Array<PropertyValue<out Any>> = emptyArray()

        fun drawable(drawable: Drawable) {
            this.drawable = drawable
        }

        fun properties(properties: Array<PropertyValue<out Any>>) {
            this.properties = properties
        }

        fun build() = RouteAlertViewOptions(
            context,
            style,
            drawable,
            properties
        )
    }
}
