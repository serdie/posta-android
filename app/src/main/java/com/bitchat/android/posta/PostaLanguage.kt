package com.bitchat.android.posta

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Posta es una app para España y está solo en español.
 *
 * El idioma por defecto de los recursos ya es el español, así que incluso un
 * móvil en catalán, gallego, euskera o inglés ve la app en español. Además se
 * fija el idioma de la app para que las pantallas no dependan del sistema.
 */
object PostaLanguage {
    const val TAG_ES = "es-ES"

    fun enforceSpanish() {
        val current = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        if (!current.startsWith("es")) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(TAG_ES))
        }
    }
}
