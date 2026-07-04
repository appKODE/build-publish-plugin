@file:Suppress("MaxLineLength")

package ru.kode.android.build.publish.plugin.telegram.messages

import ru.kode.android.build.publish.plugin.core.util.SecretRedaction

fun sendingMessageBotMessage(
    botName: String,
    webhookUrl: String,
): String = "Sending message via bot '$botName' to ${SecretRedaction.redactUrl(webhookUrl)}"

fun uploadFileStartedMessage(
    botName: String,
    webhookUrl: String,
): String = "Uploading file via bot '$botName' to ${SecretRedaction.redactUrl(webhookUrl)}"

fun failedToParseRetryMessage(bodyString: String): String =
    "Failed to parse 'retry_after' from Telegram API response. Body: ${bodyString.take(500)}"

fun tooManyRequestsMessage(
    retryAfterSeconds: Long,
    attempt: Int,
    maxRetries: Int,
): String = "Too many requests. Retrying in $retryAfterSeconds seconds (attempt $attempt of $maxRetries)"

fun reachedMaxTriesMessage(maxRetries: Int): String = "Reached maximum retry attempts ($maxRetries). Giving up."

fun botWithoutChatMessage(botName: String): String = "Bot '$botName' is missing required chat configuration (chatId is empty or missing)"

@Suppress("FunctionOnlyReturningConstant")
fun telegramBotFailedEncodeToJsonMessage(): String = "Failed to serialize TelegramBot configuration to JSON"

fun telegramBotJsonParsingFailedMessage(json: String): String = "Failed to parse Telegram bot configuration from JSON: ${json.take(200)}"

@Suppress("FunctionOnlyReturningConstant")
fun destinationBotsEncodeToJsonFailedMessage(): String = "Failed to serialize the list of destination bots to JSON"

fun destinationBotsJsonParsingFailedMessage(json: String): String =
    "Failed to parse destination bots configuration from JSON: ${json.take(200)}"
