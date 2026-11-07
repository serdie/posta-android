package com.bitchat.android.posta

/**
 * Estados de emergencia de Posta.
 *
 * Se envían como mensajes de texto normales del protocolo Bitchat, con un formato
 * fijo en español. Así:
 *  - siguen siendo compatibles: cualquier cliente Bitchat (Android o iPhone) los
 *    recibe y los entiende aunque no sepa nada de Posta;
 *  - no hay campos nuevos en el protocolo que revisar ni metadatos adicionales;
 *  - Posta los reconoce y los resalta en la conversación.
 *
 * El texto del cable es estable: no depende del idioma del teléfono que lo envía,
 * para que quien lo recibe lo reconozca siempre.
 */
enum class PostaStatus(
    val emoji: String,
    val wireEs: String,
    /** Texto inglés de las versiones 0.1–0.2, solo para reconocer mensajes antiguos. */
    private val legacyEn: String,
    val severity: Severity,
) {
    OK("🟢", "ESTOY BIEN", "I'M OK", Severity.OK),
    MEETING_POINT("📍", "VOY AL PUNTO DE ENCUENTRO", "HEADING TO MEETING POINT", Severity.INFO),
    NEED_HELP("🟠", "NECESITO AYUDA", "I NEED HELP", Severity.WARNING),
    MEDICAL("🔴", "URGENCIA MÉDICA", "MEDICAL EMERGENCY", Severity.DANGER);

    enum class Severity { OK, INFO, WARNING, DANGER }

    /** Cabecera del mensaje tal y como viaja por la malla. */
    val header: String get() = "$emoji $wireEs"

    /** Cabecera bilingüe que enviaban las versiones 0.1–0.2 de Posta. */
    internal val legacyHeader: String get() = "$emoji $wireEs / $legacyEn"

    /**
     * Texto completo a enviar, con una nota opcional ya saneada y recortada y,
     * si se quiere, las coordenadas GPS al final.
     */
    fun toWire(note: String? = null, location: PostaCoordinates? = null): String {
        val clean = sanitizeNote(note)
        val tail = listOfNotNull(clean.ifEmpty { null }, location?.toWire())
            .joinToString(PostaCoordinates.SEPARATOR)
        return if (tail.isEmpty()) header else "$header$NOTE_SEPARATOR$tail"
    }

    companion object {
        const val NOTE_SEPARATOR = " — "

        /** Longitud máxima de la nota: los mensajes cortos viajan mejor por BLE. */
        const val MAX_NOTE_LENGTH = 140

        fun sanitizeNote(note: String?): String {
            if (note == null) return ""
            val singleLine = note
                .replace(Regex("[\\p{Cntrl}&&[^\\n]]"), "")
                .replace(Regex("\\s+"), " ")
                .trim()
            return if (singleLine.length <= MAX_NOTE_LENGTH) singleLine
            else singleLine.take(MAX_NOTE_LENGTH).trimEnd()
        }

        /**
         * Reconoce un estado de Posta. Devuelve null para cualquier otro mensaje.
         * Solo acepta el formato exacto al principio del mensaje, para no confundir
         * conversaciones normales que mencionen estas palabras.
         */
        fun parse(content: String): Parsed? {
            val text = content.trimStart()
            for (status in entries) {
                // Primero el formato antiguo, que empieza igual que el nuevo pero es más largo.
                val header = listOf(status.legacyHeader, status.header).firstOrNull { text.startsWith(it) } ?: continue
                val rest = text.substring(header.length)
                if (rest.isEmpty()) return Parsed(status, null)
                if (rest.startsWith(NOTE_SEPARATOR)) {
                    val raw = rest.substring(NOTE_SEPARATOR.length).trim()
                    val (note, location) = PostaCoordinates.splitTrailing(raw.ifEmpty { null })
                    return Parsed(status, note, location)
                }
                return null
            }
            return null
        }
    }

    data class Parsed(
        val status: PostaStatus,
        val note: String?,
        val location: PostaCoordinates? = null,
    )
}
