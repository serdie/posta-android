package com.bitchat.android.posta

import android.util.Log
import com.bitchat.android.service.MeshServiceHolder
import com.bitchat.android.services.AppStateStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Aviso activo: repite un estado de Posta en el canal público de la malla.
 *
 * Por qué: un mensaje se emite una vez. Bitchat ya reenvía los mensajes
 * públicos recientes a quien llega nuevo, pero en un canal con mucho tráfico un
 * «Necesito ayuda» acaba fuera de esa ventana. Mientras el aviso esté activo,
 * se vuelve a emitir cada cierto tiempo, solo si han aparecido teléfonos nuevos
 * al alcance desde la última vez (no se gasta batería repitiéndolo a los mismos).
 *
 * Se envía como un mensaje normal, sin cambios de protocolo. Posta oculta las
 * repeticiones idénticas al recibirlas ([PostaRepeatFilter]).
 */
object PostaBeacon {
    private const val TAG = "PostaBeacon"
    private const val TICK_MS = 30_000L

    data class State(
        val content: String,
        val startedAtMs: Long,
        val lastSentAtMs: Long,
        val repeats: Int,
        val untilMs: Long,
    )

    /** Decisión pura, separada para poder probarla. */
    fun shouldRepeat(
        state: State,
        nowMs: Long,
        intervalMs: Long,
        peersNow: Set<String>,
        peersAtLastSend: Set<String>,
    ): Boolean {
        if (nowMs >= state.untilMs) return false
        if (nowMs - state.lastSentAtMs < intervalMs) return false
        return (peersNow - peersAtLastSend).isNotEmpty()
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var job: Job? = null
    private var peersAtLastSend: Set<String> = emptySet()
    private val _state = MutableStateFlow<State?>(null)
    val state: StateFlow<State?> = _state.asStateFlow()

    /** Empieza a repetir [content]. Sustituye cualquier aviso activo anterior. */
    @Synchronized
    fun start(content: String, nowMs: Long = System.currentTimeMillis()) {
        val settings = PostaSettings.values.value
        stop()
        if (!settings.beaconEnabled) return
        peersAtLastSend = AppStateStore.getDirectPeers()
        _state.value = State(
            content = content,
            startedAtMs = nowMs,
            lastSentAtMs = nowMs,
            repeats = 0,
            untilMs = nowMs + settings.beaconDurationHours * 3_600_000L,
        )
        job = scope.launch {
            while (isActive) {
                delay(TICK_MS)
                tick(System.currentTimeMillis())
            }
        }
    }

    @Synchronized
    fun stop() {
        job?.cancel()
        job = null
        _state.value = null
    }

    @Synchronized
    private fun tick(nowMs: Long) {
        val current = _state.value ?: return
        val settings = PostaSettings.values.value
        if (!settings.beaconEnabled || nowMs >= current.untilMs) {
            stop()
            return
        }
        val peersNow = AppStateStore.getDirectPeers()
        val interval = settings.beaconIntervalMin * 60_000L
        if (!shouldRepeat(current, nowMs, interval, peersNow, peersAtLastSend)) return
        val unified = MeshServiceHolder.unifiedMeshService
        val bluetooth = MeshServiceHolder.meshService
        if (unified == null && bluetooth == null) return
        try {
            if (unified != null) unified.sendMessage(current.content, emptyList(), null)
            else bluetooth?.sendMessage(current.content, emptyList(), null)
            peersAtLastSend = peersNow
            _state.value = current.copy(lastSentAtMs = nowMs, repeats = current.repeats + 1)
        } catch (e: Exception) {
            Log.w(TAG, "Repeat failed: ${e.message}")
        }
    }
}

/**
 * Oculta repeticiones idénticas de un estado de Posta del mismo remitente
 * dentro de una ventana de tiempo. Solo afecta a estados de Posta: cualquier
 * otro mensaje se muestra siempre.
 */
class PostaRepeatFilter(
    private val windowMs: Long = 3 * 3_600_000L,
    private val maxEntries: Int = 200,
) {
    private val seen = LinkedHashMap<String, Long>()

    @Synchronized
    fun isRepeat(senderKey: String, content: String, nowMs: Long): Boolean {
        if (PostaStatus.parse(content) == null) return false
        val key = "$senderKey\u0000${content.trim()}"
        val last = seen[key]
        seen.remove(key)
        seen[key] = nowMs
        while (seen.size > maxEntries) seen.remove(seen.keys.first())
        return last != null && nowMs - last < windowMs
    }

    companion object {
        val shared = PostaRepeatFilter()
    }
}
