package com.bitchat.android.posta

import com.bitchat.android.mesh.PowerManager
import com.bitchat.android.mesh.PowerProfileResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PostaConnectivityTest {

    // ---------- Aviso que se repite ----------

    private val state = PostaBeacon.State(
        content = PostaStatus.NEED_HELP.toWire("portal 3"),
        startedAtMs = 0L,
        lastSentAtMs = 0L,
        repeats = 0,
        untilMs = 2 * 3_600_000L,
    )
    private val tenMin = 10 * 60_000L

    @Test
    fun `repeats when interval passed and a new phone came in range`() {
        assertTrue(PostaBeacon.shouldRepeat(state, tenMin, tenMin, setOf("a", "b"), setOf("a")))
    }

    @Test
    fun `does not repeat to the same phones`() {
        assertFalse(PostaBeacon.shouldRepeat(state, tenMin * 3, tenMin, setOf("a"), setOf("a", "b")))
        assertFalse(PostaBeacon.shouldRepeat(state, tenMin * 3, tenMin, emptySet(), emptySet()))
    }

    @Test
    fun `respects the minimum interval`() {
        assertFalse(PostaBeacon.shouldRepeat(state, tenMin - 1, tenMin, setOf("new"), emptySet()))
    }

    @Test
    fun `stops after the configured duration`() {
        assertFalse(PostaBeacon.shouldRepeat(state, state.untilMs, tenMin, setOf("new"), emptySet()))
    }

    // ---------- Filtro de repeticiones ----------

    @Test
    fun `identical status from the same sender is hidden within the window`() {
        val f = PostaRepeatFilter(windowMs = 1_000L)
        val msg = PostaStatus.OK.toWire()
        assertFalse(f.isRepeat("peer1", msg, 0L))
        assertTrue(f.isRepeat("peer1", msg, 500L))
        // Ventana deslizante: cada repetición renueva el plazo.
        assertTrue(f.isRepeat("peer1", msg, 1_400L))
        assertFalse(f.isRepeat("peer1", msg, 2_500L))
    }

    @Test
    fun `different sender, different status or ordinary messages are never hidden`() {
        val f = PostaRepeatFilter()
        val ok = PostaStatus.OK.toWire()
        assertFalse(f.isRepeat("peer1", ok, 0L))
        assertFalse(f.isRepeat("peer2", ok, 1L))
        assertFalse(f.isRepeat("peer1", PostaStatus.NEED_HELP.toWire(), 2L))
        assertFalse(f.isRepeat("peer1", "hola", 3L))
        assertFalse(f.isRepeat("peer1", "hola", 4L))
    }

    @Test
    fun `filter memory is bounded`() {
        val f = PostaRepeatFilter(maxEntries = 3)
        val ok = PostaStatus.OK.toWire()
        for (i in 0 until 10) f.isRepeat("peer$i", ok, i.toLong())
        // peer0 fue expulsado: ya no se reconoce como repetición.
        assertFalse(f.isRepeat("peer0", ok, 20L))
    }

    // ---------- Ajustes ----------

    @Test
    fun `invalid settings fall back to recommended values`() {
        val v = PostaSettings.sanitize(
            PostaSettings.Values(beaconIntervalMin = 7, beaconDurationHours = 99, survivalThreshold = 3)
        )
        assertEquals(10, v.beaconIntervalMin)
        assertEquals(2, v.beaconDurationHours)
        assertEquals(30, v.survivalThreshold)
    }

    @Test
    fun `automatic survival depends on battery and charging`() {
        val auto = PostaSettings.Values(survivalMode = PostaSettings.SurvivalMode.AUTO, survivalThreshold = 30)
        assertTrue(PostaSettings.isSurvivalActive(auto, 30, false))
        assertFalse(PostaSettings.isSurvivalActive(auto, 31, false))
        assertFalse(PostaSettings.isSurvivalActive(auto, 10, true))
        assertTrue(PostaSettings.isSurvivalActive(auto.copy(survivalMode = PostaSettings.SurvivalMode.ALWAYS), 100, true))
        assertFalse(PostaSettings.isSurvivalActive(auto.copy(survivalMode = PostaSettings.SurvivalMode.OFF), 1, false))
    }

    // ---------- Política de energía ----------

    @Test
    fun `survival profile scans less often but keeps relaying`() {
        val fg = PowerProfileResolver.resolve(80, false, isBackground = false, hasDirectPeers = true, survival = true)
        assertTrue(fg.postaSurvival)
        assertEquals(PowerManager.PowerMode.POWER_SAVER, fg.mode)
        assertEquals(1_000L, fg.ble.scanOnMs)
        assertEquals(44_000L, fg.ble.scanOffMs)
        assertEquals(8, fg.ble.maxConnections)
        assertEquals(300_000L, fg.meshAnnouncementIntervalMs)

        val bg = PowerProfileResolver.resolve(80, false, isBackground = true, hasDirectPeers = false, survival = true)
        assertEquals(89_000L, bg.ble.scanOffMs)
    }

    @Test
    fun `without survival the upstream policy is unchanged`() {
        val normal = PowerProfileResolver.resolve(80, false, isBackground = false, hasDirectPeers = true)
        assertFalse(normal.postaSurvival)
        assertEquals(PowerManager.PowerMode.BALANCED, normal.mode)
        assertEquals(8_000L, normal.ble.scanOnMs)
    }
}
