package ru.kode.android.build.publish.plugin.firebase.remoteconfig

import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

/**
 * One `<entry><key>…</key><value>…</value></entry>` of an in-app defaults XML (`setDefaultsAsync(R.xml.…)`).
 *
 * @property path File path relative to the repository root.
 * @property line 1-based line of the `<key>` element.
 */
data class InAppDefault(
    val key: String,
    val value: String,
    val path: String,
    val line: Int,
)

/**
 * Parses the entries of an in-app defaults XML. DTDs are rejected, so external entities are never resolved.
 */
fun parseInAppDefaults(
    path: String,
    content: String,
): List<InAppDefault> {
    val factory =
        DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            isExpandEntityReferences = false
            isXIncludeAware = false
        }
    val entries = factory.newDocumentBuilder().parse(InputSource(StringReader(content))).getElementsByTagName("entry")
    return (0 until entries.length).mapNotNull { index ->
        val entry = entries.item(index) as Element
        val key = entry.child("key") ?: return@mapNotNull null
        InAppDefault(key, entry.child("value").orEmpty(), path, content.lineOfKey(key))
    }
}

private fun Element.child(tag: String) = getElementsByTagName(tag).item(0)?.textContent?.trim()

private fun String.lineOfKey(key: String): Int {
    val start = Regex("""<key>\s*${Regex.escape(key)}\s*</key>""").find(this)?.range?.first ?: 0
    return take(start).count { it == '\n' } + 1
}
