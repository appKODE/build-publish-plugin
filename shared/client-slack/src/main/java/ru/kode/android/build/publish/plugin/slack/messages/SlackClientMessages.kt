@file:Suppress("MaxLineLength")

package ru.kode.android.build.publish.plugin.slack.messages

import ru.kode.android.build.publish.plugin.core.util.SecretRedaction

fun blockTextHasMoreSymbolsMessage(maxSymbols: Int): String =
    "Message block text exceeds the maximum allowed length of $maxSymbols characters."

fun headerTextHasMoreSymbolsMessage(maxSymbols: Int): String = "Header text exceeds the maximum allowed length of $maxSymbols characters."

fun failedToSendChangelogMessage(webhookUrl: String): String {
    val redactedUrl = SecretRedaction.redactUrl(webhookUrl)
    return "Failed to send changelog to Slack webhook: $redactedUrl"
}

fun sendingChangelogMessage(
    body: String,
    webhookUrl: String,
): String = "sending $body to ${SecretRedaction.redactUrl(webhookUrl)}"
