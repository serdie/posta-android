package com.bitchat.android.posta

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.Executors
import kotlin.coroutines.resume

/**
 * Obtiene una posición GPS bajo demanda, solo cuando la persona lo pide al
 * enviar un estado. No hay seguimiento ni historial: se lee una vez y se olvida.
 *
 * Usa el proveedor GPS del sistema, que funciona sin internet ni cobertura.
 */
object PostaLocationProvider {

    sealed interface Result {
        data class Found(val coordinates: PostaCoordinates) : Result
        data object NoPermission : Result
        data object LocationOff : Result
        data object Timeout : Result
    }

    /** Una posición reciente sirve: en una emergencia nadie se ha movido mucho en 2 minutos. */
    private const val FRESH_MS = 2 * 60 * 1000L
    const val DEFAULT_TIMEOUT_MS = 30_000L

    private val executor = Executors.newSingleThreadExecutor()

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun isLocationOn(context: Context): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return LocationManagerCompat.isLocationEnabled(lm)
    }

    @SuppressLint("MissingPermission") // comprobado en hasPermission()
    suspend fun current(context: Context, timeoutMs: Long = DEFAULT_TIMEOUT_MS): Result {
        if (!hasPermission(context)) return Result.NoPermission
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return Result.LocationOff
        if (!LocationManagerCompat.isLocationEnabled(lm)) return Result.LocationOff

        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
        if (providers.isEmpty()) return Result.LocationOff

        val recent = providers
            .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .filter { ageMs(it) in 0..FRESH_MS }
            .minByOrNull { if (it.hasAccuracy()) it.accuracy else Float.MAX_VALUE }

        val provider = if (LocationManager.GPS_PROVIDER in providers) LocationManager.GPS_PROVIDER else providers.first()
        val fresh = withTimeoutOrNull(timeoutMs) { request(lm, provider) }
        val best = fresh ?: recent ?: return Result.Timeout
        return Result.Found(
            PostaCoordinates.of(best.latitude, best.longitude, if (best.hasAccuracy()) best.accuracy else null)
        )
    }

    private fun ageMs(location: Location): Long =
        (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000

    @SuppressLint("MissingPermission")
    private suspend fun request(lm: LocationManager, provider: String): Location? =
        suspendCancellableCoroutine { cont ->
            val signal = CancellationSignal()
            cont.invokeOnCancellation { signal.cancel() }
            try {
                LocationManagerCompat.getCurrentLocation(lm, provider, signal, executor) { location ->
                    if (cont.isActive) cont.resume(location)
                }
            } catch (_: Exception) {
                if (cont.isActive) cont.resume(null)
            }
        }
}
