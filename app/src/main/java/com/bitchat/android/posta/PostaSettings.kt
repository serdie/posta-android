package com.bitchat.android.posta

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Ajustes avanzados de conectividad de Posta.
 *
 * Los valores por defecto están pensados para una emergencia real: quien no
 * toque nada tiene avisos que se repiten y ahorro automático de batería.
 * Todo se guarda solo en el teléfono.
 */
object PostaSettings {

    enum class SurvivalMode { AUTO, ALWAYS, OFF }

    data class Values(
        /** Repetir los estados enviados al canal público mientras estén activos. */
        val beaconEnabled: Boolean = true,
        val beaconIntervalMin: Int = 10,
        val beaconDurationHours: Int = 2,
        /** Ocultar las repeticiones idénticas que llegan de otros teléfonos. */
        val collapseRepeats: Boolean = true,
        val survivalMode: SurvivalMode = SurvivalMode.AUTO,
        /** En automático, por debajo de este porcentaje se activa el modo supervivencia. */
        val survivalThreshold: Int = 30,
    )

    val BEACON_INTERVALS_MIN = listOf(5, 10, 15, 30)
    val BEACON_DURATIONS_H = listOf(1, 2, 6, 12)
    val SURVIVAL_THRESHOLDS = listOf(20, 30, 50)

    private const val PREFS = "posta_settings"
    private var prefs: SharedPreferences? = null
    private val _values = MutableStateFlow(Values())
    val values: StateFlow<Values> = _values.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        val d = Values()
        _values.value = sanitize(
            Values(
                beaconEnabled = p.getBoolean("beacon_enabled", d.beaconEnabled),
                beaconIntervalMin = p.getInt("beacon_interval_min", d.beaconIntervalMin),
                beaconDurationHours = p.getInt("beacon_duration_h", d.beaconDurationHours),
                collapseRepeats = p.getBoolean("collapse_repeats", d.collapseRepeats),
                survivalMode = runCatching {
                    SurvivalMode.valueOf(p.getString("survival_mode", d.survivalMode.name)!!)
                }.getOrDefault(d.survivalMode),
                survivalThreshold = p.getInt("survival_threshold", d.survivalThreshold),
            )
        )
    }

    /** Solo admite los valores ofrecidos en la pantalla; cualquier otro vuelve al defecto. */
    fun sanitize(v: Values): Values {
        val d = Values()
        return v.copy(
            beaconIntervalMin = v.beaconIntervalMin.takeIf { it in BEACON_INTERVALS_MIN } ?: d.beaconIntervalMin,
            beaconDurationHours = v.beaconDurationHours.takeIf { it in BEACON_DURATIONS_H } ?: d.beaconDurationHours,
            survivalThreshold = v.survivalThreshold.takeIf { it in SURVIVAL_THRESHOLDS } ?: d.survivalThreshold,
        )
    }

    fun update(transform: (Values) -> Values) {
        val next = sanitize(transform(_values.value))
        _values.value = next
        prefs?.edit()
            ?.putBoolean("beacon_enabled", next.beaconEnabled)
            ?.putInt("beacon_interval_min", next.beaconIntervalMin)
            ?.putInt("beacon_duration_h", next.beaconDurationHours)
            ?.putBoolean("collapse_repeats", next.collapseRepeats)
            ?.putString("survival_mode", next.survivalMode.name)
            ?.putInt("survival_threshold", next.survivalThreshold)
            ?.apply()
    }

    /** Decide si el modo supervivencia debe estar activo ahora mismo. */
    fun isSurvivalActive(values: Values, batteryLevel: Int, isCharging: Boolean): Boolean =
        when (values.survivalMode) {
            SurvivalMode.ALWAYS -> true
            SurvivalMode.OFF -> false
            SurvivalMode.AUTO -> !isCharging && batteryLevel in 0..values.survivalThreshold
        }
}
