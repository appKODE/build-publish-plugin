package ru.kode.android.build.publish.plugin.core.util

/**
 * Masks secrets that would otherwise appear in log output (and thus in CI-uploaded test reports):
 * credential-bearing URL segments (Slack webhook token, Telegram bot token) and raw credential values.
 */
object SecretRedaction {
    const val REDACTED = "██"

    private val SENSITIVE_URL_PATTERNS =
        listOf(
            // Telegram bot token: .../bot<id>:<token>/...
            Regex("""/bot\d+:[A-Za-z0-9_-]+"""),
            // Slack incoming-webhook / upload path carrying a secret token
            Regex("""services/[A-Za-z0-9]+/[A-Za-z0-9]+/[A-Za-z0-9]+"""),
        )

    /** Replaces sensitive token segments inside a URL (or a message containing one), leaving the host visible. */
    fun redactUrl(url: String): String {
        var result = url
        SENSITIVE_URL_PATTERNS.forEach { regex -> result = regex.replace(result, REDACTED) }
        return result
    }

    /** Fully masks a raw credential value (user name, token, password). */
    fun redactCredential(value: String): String = if (value.isEmpty()) value else REDACTED
}
