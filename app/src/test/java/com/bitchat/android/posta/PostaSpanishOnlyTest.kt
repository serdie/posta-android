package com.bitchat.android.posta

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Posta está solo en español. Estos tests fallan si una actualización de Bitchat
 * trae textos nuevos en inglés sin traducir, o si vuelve otro idioma.
 */
class PostaSpanishOnlyTest {

    private val res = listOf("src/main/res", "app/src/main/res").map(::File).first { it.exists() }

    private fun entries(file: File): Map<String, Pair<String, String>> =
        Regex("""<(string|plurals|string-array) name="([^"]+)"([^>]*)>(.*?)</\1>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(file.readText())
            .associate { it.groupValues[2] to (it.groupValues[3] to it.groupValues[4].trim()) }

    @Test
    fun `default resources are the Spanish ones`() {
        for (name in listOf("strings.xml", "strings_posta.xml")) {
            val base = entries(File(res, "values/$name"))
            val es = entries(File(res, "values-es/$name"))
            for ((key, attrsAndValue) in base) {
                val (attrs, value) = attrsAndValue
                if ("translatable=\"false\"" in attrs) continue
                assertTrue("$name: falta «$key» en values-es", key in es)
                assertEquals("$name: «$key» difiere entre values y values-es", es.getValue(key).second, value)
            }
        }
    }

    @Test
    fun `only Spanish is shipped`() {
        val locales = res.listFiles().orEmpty()
            .filter { it.isDirectory && it.name.startsWith("values-") }
            .map { it.name }
            .filterNot { it == "values-night" || Regex("""values-v\d+""").matches(it) }
        assertEquals(listOf("values-es"), locales)
    }

    @Test
    fun `common English words do not appear in visible texts`() {
        val english = Regex("""\b(the|your|please|tap|failed|unable|enable|allow|download|settings|message|messages)\b""", RegexOption.IGNORE_CASE)
        val offenders = entries(File(res, "values/strings.xml")).filter { (_, v) ->
            "translatable=\"false\"" !in v.first && english.containsMatchIn(v.second)
        }.keys
        assertTrue("Textos que parecen en inglés: $offenders", offenders.isEmpty())
    }
}
