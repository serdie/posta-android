package com.bitchat.android.posta

import java.util.Locale
import kotlin.math.roundToInt

/**
 * Coordenadas que acompañan a un estado de Posta.
 *
 * El GPS funciona sin internet ni cobertura: el teléfono escucha a los satélites
 * directamente. Mandar unas coordenadas junto a «Necesito ayuda» permite que
 * alguien llegue aunque no haya calles reconocibles ni mapas en línea.
 *
 * Formato en el cable (texto normal, legible en cualquier cliente Bitchat):
 *   `📍 40.41680,-3.70380 (±12 m)`
 * Cinco decimales ≈ 1 metro. Siempre con punto decimal, sea cual sea el idioma.
 */
data class PostaCoordinates(
    val latitude: Double,
    val longitude: Double,
    /** Precisión estimada en metros, si se conoce. */
    val accuracyMeters: Int? = null,
) {
    init {
        require(latitude in -90.0..90.0) { "Latitud fuera de rango" }
        require(longitude in -180.0..180.0) { "Longitud fuera de rango" }
        require(accuracyMeters == null || accuracyMeters >= 0) { "Precisión negativa" }
    }

    fun toWire(): String {
        val base = String.format(Locale.US, "%s %.5f,%.5f", MARKER, latitude, longitude)
        return if (accuracyMeters == null) base else "$base (±$accuracyMeters m)"
    }

    /** URI estándar que abre cualquier app de mapas, incluidas las que funcionan sin conexión. */
    fun toGeoUri(): String {
        val lat = String.format(Locale.US, "%.5f", latitude)
        val lon = String.format(Locale.US, "%.5f", longitude)
        return "geo:$lat,$lon?q=$lat,$lon"
    }

    companion object {
        const val MARKER = "📍"
        const val SEPARATOR = " · "

        private val TRAILING = Regex(
            """(?:^|\s·\s)📍 (-?\d{1,2}\.\d{1,6}),(-?\d{1,3}\.\d{1,6})(?: \(±(\d{1,6}) m\))?$"""
        )

        fun of(latitude: Double, longitude: Double, accuracy: Float?): PostaCoordinates =
            PostaCoordinates(
                latitude = latitude,
                longitude = longitude,
                accuracyMeters = accuracy?.takeIf { it.isFinite() && it >= 0f }?.roundToInt(),
            )

        /**
         * Separa unas coordenadas al final de una nota.
         * Devuelve la nota sin ellas (o null si queda vacía) y las coordenadas, si las hay.
         */
        fun splitTrailing(note: String?): Pair<String?, PostaCoordinates?> {
            if (note.isNullOrEmpty()) return null to null
            val match = TRAILING.find(note) ?: return note to null
            val (lat, lon, acc) = match.destructured
            val coordinates = runCatching {
                PostaCoordinates(lat.toDouble(), lon.toDouble(), acc.toIntOrNull())
            }.getOrNull() ?: return note to null
            val text = note.substring(0, match.range.first).trim().ifEmpty { null }
            return text to coordinates
        }
    }
}
