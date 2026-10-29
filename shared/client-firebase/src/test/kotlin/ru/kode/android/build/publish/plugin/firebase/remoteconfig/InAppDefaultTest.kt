package ru.kode.android.build.publish.plugin.firebase.remoteconfig

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.xml.sax.SAXParseException

class InAppDefaultTest {
    @Test
    fun `should parse entries with key lines`() {
        val xml =
            """
            <?xml version="1.0" encoding="utf-8"?>
            <defaultsMap>
                <entry>
                    <key>flag</key>
                    <value>true</value>
                </entry>
                <entry><key> spaced </key><value/></entry>
            </defaultsMap>
            """.trimIndent()

        assertEquals(
            listOf(InAppDefault("flag", "true", "d.xml", 4), InAppDefault("spaced", "", "d.xml", 7)),
            parseInAppDefaults("d.xml", xml),
        )
    }

    @Test
    fun `should reject doctype so external entities are never resolved`() {
        val xml =
            """
            <?xml version="1.0"?>
            <!DOCTYPE d [<!ENTITY x SYSTEM "file:///etc/passwd">]>
            <defaultsMap><entry><key>k</key><value>&x;</value></entry></defaultsMap>
            """.trimIndent()

        assertThrows<SAXParseException> { parseInAppDefaults("d.xml", xml) }
    }
}
