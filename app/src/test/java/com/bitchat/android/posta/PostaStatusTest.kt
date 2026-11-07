package com.bitchat.android.posta

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PostaStatusTest {

    @Test
    fun `every status round-trips without a note`() {
        for (status in PostaStatus.entries) {
            val parsed = PostaStatus.parse(status.toWire())
            assertEquals(status, parsed?.status)
            assertNull(parsed?.note)
        }
    }

    @Test
    fun `note round-trips and is trimmed`() {
        val wire = PostaStatus.NEED_HELP.toWire("  portal 3, segundo piso  ")
        assertEquals("🟠 NECESITO AYUDA / I NEED HELP — portal 3, segundo piso", wire)
        val parsed = PostaStatus.parse(wire)
        assertEquals(PostaStatus.NEED_HELP, parsed?.status)
        assertEquals("portal 3, segundo piso", parsed?.note)
    }

    @Test
    fun `wire format is stable and bilingual`() {
        assertEquals("🟢 ESTOY BIEN / I'M OK", PostaStatus.OK.toWire())
        assertEquals("🔴 URGENCIA MÉDICA / MEDICAL EMERGENCY", PostaStatus.MEDICAL.toWire(""))
    }

    @Test
    fun `ordinary messages are not statuses`() {
        assertNull(PostaStatus.parse("estoy bien, gracias"))
        assertNull(PostaStatus.parse("hola 🟢 ESTOY BIEN / I'M OK"))
        assertNull(PostaStatus.parse("🟢 ESTOY BIENVENIDO"))
        assertNull(PostaStatus.parse("🟢 ESTOY BIEN / I'M OKAY"))
        assertNull(PostaStatus.parse(""))
    }

    @Test
    fun `leading whitespace is tolerated`() {
        assertEquals(PostaStatus.OK, PostaStatus.parse("  🟢 ESTOY BIEN / I'M OK")?.status)
    }

    @Test
    fun `empty note after separator is treated as no note`() {
        val parsed = PostaStatus.parse("🟢 ESTOY BIEN / I'M OK — ")
        assertEquals(PostaStatus.OK, parsed?.status)
        assertNull(parsed?.note)
    }

    @Test
    fun `notes are single line, without control chars, and capped`() {
        val long = "a".repeat(500)
        assertEquals(PostaStatus.MAX_NOTE_LENGTH, PostaStatus.sanitizeNote(long).length)
        assertEquals("uno dos", PostaStatus.sanitizeNote("uno\n\u0007dos"))
        assertEquals("", PostaStatus.sanitizeNote(null))
        assertTrue(PostaStatus.OK.toWire(long).length <= PostaStatus.OK.header.length + 3 + PostaStatus.MAX_NOTE_LENGTH)
    }
}
