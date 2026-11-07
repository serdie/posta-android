package com.bitchat.android.posta

/**
 * Patrón SOS en código Morse (··· ––– ···), la señal de socorro reconocida
 * internacionalmente con luz, sonido o golpes.
 *
 * Tiempos estándar de Morse: punto = 1 unidad, raya = 3, silencio entre
 * señales = 1, entre letras = 3 y entre repeticiones (palabras) = 7.
 */
object SosPattern {
    /** Unidad de tiempo. 250 ms es lento y fácil de reconocer a distancia. */
    const val UNIT_MS = 250L

    data class Step(val on: Boolean, val durationMs: Long)

    private const val SOS = "... --- ..."

    /** Una repetición completa de SOS, terminada en la pausa larga antes de repetir. */
    fun cycle(unitMs: Long = UNIT_MS): List<Step> {
        require(unitMs > 0) { "La unidad debe ser positiva" }
        val steps = mutableListOf<Step>()
        val letters = SOS.split(' ')
        letters.forEachIndexed { li, letter ->
            letter.forEachIndexed { si, symbol ->
                steps += Step(on = true, durationMs = if (symbol == '.') unitMs else unitMs * 3)
                if (si < letter.lastIndex) steps += Step(on = false, durationMs = unitMs)
            }
            steps += Step(on = false, durationMs = if (li < letters.lastIndex) unitMs * 3 else unitMs * 7)
        }
        return steps
    }

    fun cycleDurationMs(unitMs: Long = UNIT_MS): Long = cycle(unitMs).sumOf { it.durationMs }
}
