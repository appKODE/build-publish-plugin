package ru.kode.android.build.publish.plugin.firebase.remoteconfig

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.kode.android.build.publish.plugin.firebase.remoteconfig.extractor.KeyOccurrence
import ru.kode.android.build.publish.plugin.firebase.remoteconfig.extractor.RegexKeyExtractor

class RegexKeyExtractorTest {
    @Test
    fun `should extract keys with lines from multiline declarations`() {
        val content =
            """
            sealed class AppFeature(val id: String)
            data object Search : AppFeature("search_enabled")
            data object Map :
                AppFeature(
                    id = "map_v2",
                )
            """.trimIndent()
        val extractor = RegexKeyExtractor("""data object\s+\w+\s*:\s*AppFeature\(\s*(?:id\s*=\s*)?"([^"]+)"""")

        assertEquals(
            listOf(
                KeyOccurrence("search_enabled", "AppFeature.kt", 2),
                KeyOccurrence("map_v2", "AppFeature.kt", 3),
            ),
            extractor.extract("AppFeature.kt", content),
        )
    }

    @Test
    fun `should take first matched group when pattern has several groups`() {
        val extractor = RegexKeyExtractor("""KEY\("([^"]+)"\)|key = '([^']+)'""")

        assertEquals(
            listOf("a", "b"),
            extractor.extract("F.kt", "KEY(\"a\")\nkey = 'b'").map { it.key },
        )
    }

    @Test
    fun `should return nothing when pattern does not match`() {
        assertEquals(emptyList<KeyOccurrence>(), RegexKeyExtractor("""KEY\("(\w+)"\)""").extract("F.kt", "val x = 1"))
    }

    @Test
    fun `should extract keys from remote config defaults xml`() {
        val xml =
            """
            <defaultsMap>
                <entry>
                    <key>welcome_text</key>
                    <value>Hi</value>
                </entry>
                <entry><key> promo_enabled </key><value>false</value></entry>
            </defaultsMap>
            """.trimIndent()

        assertEquals(
            listOf(
                KeyOccurrence("welcome_text", "defaults.xml", 3),
                KeyOccurrence("promo_enabled", "defaults.xml", 6),
            ),
            RegexKeyExtractor.DEFAULTS_XML.extract("defaults.xml", xml),
        )
    }
}
