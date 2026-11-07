package com.bitchat.android.posta

import com.bitchat.android.util.AppConstants
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Posta tiene que poder instalarse en un móvil que ya tenga Bitchat.
 *
 * Android rechaza la instalación (INSTALL_FAILED_DUPLICATE_PERMISSION /
 * CONFLICTING_PROVIDER) si dos apps firmadas con claves distintas declaran un
 * permiso o un proveedor con el mismo nombre. Todo lo que declare Posta debe
 * llevar su propio prefijo.
 */
class PostaCoexistenceTest {

    private val manifest: String = listOf("src/main/AndroidManifest.xml", "app/src/main/AndroidManifest.xml")
        .map(::File).first { it.exists() }.readText()

    @Test
    fun `declared permissions use the Posta namespace`() {
        val declared = Regex("""<permission\s+[^>]*?android:name="([^"]+)"""", RegexOption.DOT_MATCHES_ALL)
            .findAll(manifest).map { it.groupValues[1] }.toList()
        assertTrue("No se encontró ningún <permission> que comprobar", declared.isNotEmpty())
        for (name in declared) {
            assertTrue("Permiso con nombre ajeno a Posta: $name", name.startsWith("org.posta.app."))
        }
    }

    @Test
    fun `provider authorities derive from the application id`() {
        val authorities = Regex("""android:authorities="([^"]+)"""").findAll(manifest).map { it.groupValues[1] }.toList()
        for (a in authorities) {
            assertTrue("Proveedor con nombre fijo: $a", a.startsWith("\${applicationId}"))
        }
    }

    @Test
    fun `internal broadcast names are Posta's own`() {
        assertTrue(AppConstants.UI.PERMISSION_FORCE_FINISH.startsWith("org.posta.app."))
        assertTrue(AppConstants.UI.ACTION_FORCE_FINISH.startsWith("org.posta.app."))
    }
}
