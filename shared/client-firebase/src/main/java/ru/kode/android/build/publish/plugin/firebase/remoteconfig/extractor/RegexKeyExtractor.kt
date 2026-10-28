package ru.kode.android.build.publish.plugin.firebase.remoteconfig.extractor

/**
 * Extracts keys with a regular expression: every match yields its first non-null capture group
 * (or the whole match when the pattern has no groups). The pattern may span several lines.
 */
data class RegexKeyExtractor(
    val pattern: String,
) : RemoteConfigKeyExtractor {
    override fun extract(
        path: String,
        content: String,
    ): List<KeyOccurrence> =
        Regex(pattern).findAll(content).map { match ->
            val key = match.groups.drop(1).firstOrNull { it != null }?.value ?: match.value
            val line = content.subSequence(0, match.range.first).count { it == '\n' } + 1
            KeyOccurrence(key, path, line)
        }.toList()

    companion object {
        /**
         * Keys of a Firebase in-app defaults file (`res/xml/remote_config_defaults.xml`).
         */
        val DEFAULTS_XML = RegexKeyExtractor("""<key>\s*([^<\s]+)\s*</key>""")

        private const val serialVersionUID = 1L
    }
}
