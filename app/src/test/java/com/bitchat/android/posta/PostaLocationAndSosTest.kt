package com.bitchat.android.posta

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class PostaLocationAndSosTest {

    // Coordenadas sintéticas: no corresponden a ninguna persona ni dispositivo.
    private val point = PostaCoordinates(10.12345, -20.54321, 12)

    @Test
    fun `coordinates use a dot and five decimals whatever the locale`() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("es-ES"))
            assertEquals("📍 10.12345,-20.54321 (±12 m)", point.toWire())
            assertEquals("geo:10.12345,-20.54321?q=10.12345,-20.54321", point.toGeoUri())
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun `status with note and location round-trips`() {
        val wire = PostaStatus.NEED_HELP.toWire("portal 3", point)
        assertEquals("🟠 NECESITO AYUDA — portal 3 · 📍 10.12345,-20.54321 (±12 m)", wire)
        val parsed = PostaStatus.parse(wire)!!
        assertEquals(PostaStatus.NEED_HELP, parsed.status)
        assertEquals("portal 3", parsed.note)
        assertEquals(point, parsed.location)
    }

    @Test
    fun `status with only location round-trips`() {
        val wire = PostaStatus.MEDICAL.toWire(null, PostaCoordinates(-1.5, 2.25))
        assertEquals("🔴 URGENCIA MÉDICA — 📍 -1.50000,2.25000", wire)
        val parsed = PostaStatus.parse(wire)!!
        assertNull(parsed.note)
        assertEquals(PostaCoordinates(-1.5, 2.25), parsed.location)
    }

    @Test
    fun `location in the middle of a note is not extracted`() {
        val parsed = PostaStatus.parse("🟢 ESTOY BIEN — 📍 1.00000,2.00000 era donde estaba antes")!!
        assertNull(parsed.location)
    }

    @Test
    fun `out of range coordinates are left as plain text`() {
        val (note, location) = PostaCoordinates.splitTrailing("nota · 📍 95.00000,10.00000")
        assertEquals("nota · 📍 95.00000,10.00000", note)
        assertNull(location)
    }

    @Test
    fun `accuracy is rounded and ignored when invalid`() {
        assertEquals(8, PostaCoordinates.of(0.0, 0.0, 7.6f).accuracyMeters)
        assertNull(PostaCoordinates.of(0.0, 0.0, Float.NaN).accuracyMeters)
        assertNull(PostaCoordinates.of(0.0, 0.0, null).accuracyMeters)
    }

    @Test
    fun `sos pattern is three short, three long, three short`() {
        val on = SosPattern.cycle(100).filter { it.on }.map { it.durationMs }
        assertEquals(listOf(100L, 100L, 100L, 300L, 300L, 300L, 100L, 100L, 100L), on)
    }

    @Test
    fun `sos timing follows morse gaps`() {
        val steps = SosPattern.cycle(100)
        // Alterna encendido/apagado y termina con la pausa de palabra (7 unidades).
        steps.zipWithNext().forEach { (a, b) -> assertTrue(a.on != b.on) }
        assertEquals(700L, steps.last().durationMs)
        // 9 señales: 3·1 + 3·3 + 3·1 = 15; huecos internos 6·1; entre letras 2·3; final 7 → 34 unidades.
        assertEquals(3400L, SosPattern.cycleDurationMs(100))
    }
}
