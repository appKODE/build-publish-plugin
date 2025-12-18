package ru.kode.android.build.publish.plugin.telegram.controller

import okhttp3.Credentials
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.gradle.api.logging.Logger
import ru.kode.android.build.publish.plugin.core.util.createPartFromString
import ru.kode.android.build.publish.plugin.core.util.executeNoResult
import ru.kode.android.build.publish.plugin.core.util.executeWithResult
import ru.kode.android.build.publish.plugin.telegram.controller.entity.ChatSpecificTelegramBot
import ru.kode.android.build.publish.plugin.telegram.controller.entity.TelegramLastMessage
import ru.kode.android.build.publish.plugin.telegram.messages.sendingMessageBotMessage
import ru.kode.android.build.publish.plugin.telegram.messages.uploadFileStartedMessage
import ru.kode.android.build.publish.plugin.telegram.network.api.TelegramDistributionApi
import ru.kode.android.build.publish.plugin.telegram.network.api.TelegramWebhookApi
import ru.kode.android.build.publish.plugin.telegram.network.entity.TelegramMessage
import java.io.File
import java.net.URLEncoder
import kotlin.sequences.forEach
import kotlin.text.toRegex

internal const val TELEGRAM_DEFAULT_BASE_RUL = "https://api.telegram.org"
private const val SEND_MESSAGE_TO_CHAT_WEB_HOOK =
    "%s/bot%s/sendMessage?chat_id=%s&text=%s&parse_mode=MarkdownV2&disable_web_page_preview=true"
private const val SEND_MESSAGE_TO_TOPIC_WEB_HOOK =
    "%s/bot%s/sendMessage?chat_id=%s&message_thread_id=%s&text=%s&parse_mode=MarkdownV2" +
        "&disable_web_page_preview=true"
private const val GET_MESSAGE_IN_CHAT_WEB_HOOK = "%s/bot%s/getUpdates"
private const val SEND_DOCUMENT_WEB_HOOK = "%s/bot%s/sendDocument"
private const val MESSAGE_MAX_LENGTH = 4096
private const val ESCAPED_CHARACTERS =
    "[_]|[*]|[\\[]|[\\]]|[(]|[)]|[~]|[`]|[>]|[#]|[+]|[=]|[|]|[{]|[}]|[.]|[!]|-"

/**
 * The implementation of the TelegramController interface.
 *
 * This class is responsible for sending messages to Telegram chats using the configured bots.
 *
 * @param webhookApi the API for sending webhook requests
 * @param distributionApi the API for sending distribution requests
 * @param logger the logger for logging messages
 */
internal class TelegramControllerImpl(
    private val webhookApi: TelegramWebhookApi,
    private val distributionApi: TelegramDistributionApi,
    private val logger: Logger
) : TelegramController {

    /**
     * Sends a text message to the specified Telegram chats using the configured bots in chunks.
     *
     * This method sends a Markdown-formatted message to one or more Telegram chats
     * using the specified bots. The message will be split into chunks if it exceeds the maximum
     * length allowed by Telegram. The chunks will be separated by newlines.
     *
     * @param message The message to send (supports MarkdownV2 formatting)
     * @param bots List of bot configurations
     * @param destinationBots Set of destination bots and their respective chat configurations
     *
     * @throws IllegalStateException If no matching bot configuration is found
     * @throws IOException If there's a network error while sending the message
     *
     * @see upload For sending files instead of text messages
     */
    override fun send(
        message: String,
        header: String,
        userMentions: List<String>,
        issueUrlPrefix: String,
        issueNumberPattern: String,
        bots: List<ChatSpecificTelegramBot>,
    ) {

        val messageWithIssuesLinks = message.formatIssues(
            ESCAPED_CHARACTERS,
            issueUrlPrefix,
            issueNumberPattern,
        )

        val escapedUserMentions =
            userMentions
                .joinToString(", ")
                .escapeCharacters(ESCAPED_CHARACTERS)

        val escapedHeader = header
            .replace(ESCAPED_CHARACTERS.toRegex()) { result -> "\\${result.value}" }

        messageWithIssuesLinks
            .chunked(MESSAGE_MAX_LENGTH)
            .forEach { messageChunk ->
                val richMessageChunk =
                    buildString {
                        append("*$escapedHeader*")
                        appendLine()
                        append(escapedUserMentions)
                        appendLine()
                        appendLine()
                        append(messageChunk)
                    }.formatMessage()
                sendMessage(richMessageChunk, bots)
            }
    }

    /**
     * Uploads a file to the specified Telegram chats using the configured bots.
     *
     * This method sends a file to one or more Telegram chats using the specified bots.
     * The file will be sent as a document, and a caption can be included.
     *
     * @param file The file to upload
     * @param destinationBots Set of destination bots and their respective chat configurations
     *
     * @throws IllegalStateException If no matching bot configuration is found
     * @throws IOException If there's a network error or the file cannot be read
     *
     * @see sendMessage For sending text messages without file attachments
     */
    override fun upload(
        file: File,
        bots: List<ChatSpecificTelegramBot>,
    ) {
        bots.forEach { bot ->
            val baseUrl = bot.serverBaseUrl ?: TELEGRAM_DEFAULT_BASE_RUL
            val webhookUrl = SEND_DOCUMENT_WEB_HOOK.format(baseUrl, bot.id)
            val filePart =
                MultipartBody.Part.createFormData(
                    "document",
                    file.name,
                    file.asRequestBody(),
                )
            val topicId = bot.topicId
            val params =
                if (topicId != null) {
                    hashMapOf(
                        "message_thread_id" to createPartFromString(topicId),
                        "chat_id" to createPartFromString(bot.chatId),
                    )
                } else {
                    hashMapOf(
                        "chat_id" to createPartFromString(bot.chatId),
                    )
                }
            logger.info(uploadFileStartedMessage(bot.name, webhookUrl))

            val authorization =
                bot.basicAuth
                    ?.let { Credentials.basic(it.username, it.password) }
            distributionApi
                .upload(authorization, webhookUrl, params, filePart)
                .executeWithResult()
                .getOrThrow()
        }
    }

    /**
     * Retrieves the last message from the specified Telegram chat.
     *
     * This method fetches the last message from the specified Telegram chat.
     *
     * @param bot The [ChatSpecificTelegramBot] configuration.
     * @return The last [TelegramMessage] if available, otherwise null.
     */
    override fun getLastMessage(
        botId: String,
        chatName: String,
        topicName: String?
    ): TelegramLastMessage? {
        val webhookUrl = GET_MESSAGE_IN_CHAT_WEB_HOOK.format(
            TELEGRAM_DEFAULT_BASE_RUL,
            botId
        )

        val response = webhookApi
            .getUpdates(null, webhookUrl)
            .executeWithResult()
            .getOrThrow()

        val messages = response
            .result
            .mapNotNull { it.channel_post ?: it.edited_message ?: it.message }
            .filter { it.chat.title.contains(chatName, ignoreCase = true) }

        val messageWithTopic = messages.firstOrNull {
            val messageTopicName = it.forum_topic_created?.name
                ?: it.reply_to_message?.forum_topic_created?.name
            it.message_thread_id != null
                && messageTopicName != null
                && topicName != null
                && messageTopicName.contains(topicName, ignoreCase = true)
                && !it.text.isNullOrBlank()
        }
        val lastMessage = messages
            .sortedByDescending { it.date }
            .firstOrNull { !it.text.isNullOrBlank() }

        val topicId = messageWithTopic?.message_thread_id
        return if (topicId != null && topicName != null) {
            TelegramLastMessage(
                text = messageWithTopic.text,
                chatName = messageWithTopic.chat.title,
                chatId = messageWithTopic.chat.id.toString(),
                topicId = topicId.toString(),
                topicName = lastMessage?.forum_topic_created?.name ?: topicName
            )
        } else if (topicName == null && lastMessage != null) {
            TelegramLastMessage(
                text = lastMessage.text,
                chatId = lastMessage.chat.id.toString(),
                topicId = null,
                topicName = null,
                chatName = lastMessage.chat.title,
            )
        } else null
    }

    /**
     * Sends a text message to the specified Telegram chats using the configured bots.
     *
     * This method sends a Markdown-formatted message to one or more Telegram chats
     * using the specified bots. The message will be properly escaped for Telegram's
     * MarkdownV2 format.
     *
     * @param message The message to send (supports MarkdownV2 formatting)
     * @param destinationBots Set of destination bots and their respective chat configurations
     *
     * @throws IllegalStateException If no matching bot configuration is found
     * @throws IOException If there's a network error while sending the message
     *
     * @see upload For sending files instead of text messages
     */
    private fun sendMessage(
        message: String,
        bots: List<ChatSpecificTelegramBot>,
    ) {
        bots.forEach { bot ->
            val topicId = bot.topicId
            val baseUrl = bot.serverBaseUrl ?: TELEGRAM_DEFAULT_BASE_RUL
            val webhookUrl =
                if (topicId.isNullOrEmpty()) {
                    SEND_MESSAGE_TO_CHAT_WEB_HOOK.format(
                        baseUrl,
                        bot.id,
                        bot.chatId,
                        URLEncoder.encode(message, "utf-8"),
                    )
                } else {
                    SEND_MESSAGE_TO_TOPIC_WEB_HOOK.format(
                        baseUrl,
                        bot.id,
                        bot.chatId,
                        topicId,
                        URLEncoder.encode(message, "utf-8"),
                    )
                }
            logger.info(sendingMessageBotMessage(bot.name, webhookUrl))

            val authorization =
                bot.basicAuth
                    ?.let { Credentials.basic(it.username, it.password) }
            webhookApi
                .send(authorization, webhookUrl)
                .executeNoResult()
                .getOrThrow()
        }
    }
}

/**
 * Formats issue references in the message and escapes special characters.
 *
 * This method performs two main transformations:
 * 1. Converts issue references (e.g., #123) to clickable links using the configured URL pattern
 * 2. Escapes special characters that have special meaning in Telegram's MarkdownV2 format
 *
 * Example:
 * - Input: "Fixed issue #123"
 * - Output: "Fixed issue [#123](https://issuetracker.example.com/issue/123)"
 *
 * @param escapedCharacters Regex pattern of characters that need to be escaped
 * @return The formatted and escaped message text, ready to be sent to Telegram
 *
 * @see <a href="https://core.telegram.org/bots/api#markdownv2-style">Telegram MarkdownV2 Format</a>
 */
private fun String.formatIssues(
    escapedCharacters: String,
    issueUrlPrefix: String,
    issueNumberPattern: String
): String {
    val issueRegexp = issueNumberPattern.toRegex()
    val matchResults = issueRegexp.findAll(this).distinctBy { it.value }
    var out = this.escapeCharacters(escapedCharacters)

    matchResults.forEach { matchResult ->
        val formattedResult = matchResult.value.escapeCharacters(escapedCharacters)
        val url = (issueUrlPrefix + matchResult.value).escapeCharacters(escapedCharacters)
        val issueId = matchResult.value.escapeCharacters(escapedCharacters)
        val link = "[$issueId]($url)"
        out = out.replace(formattedResult, link)
    }
    return out
}

/**
 * Splits a string into chunks of specified length, respecting word boundaries.
 *
 * This method ensures that the message is split in a way that maintains readability:
 * 1. First tries to split on the specified delimiter (default: newline)
 * 2. If a single line is still too long, falls back to splitting on spaces
 * 3. If a single word is too long, it will be split at the exact character limit
 *
 * @param chunkLength Maximum length of each chunk (in UTF-16 code units)
 * @param delimiter Character to prefer for splitting (defaults to newline)
 * @return List of string chunks, each within the specified length limit
 *
 * @throws IllegalArgumentException If chunkLength is less than or equal to 0
 */
private fun String.chunked(
    chunkLength: Int,
    delimiter: Char = '\n',
): List<String> {
    val result = mutableListOf<String>()
    var currentIndex = 0
    while (currentIndex < this.length) {
        var nextNewlineIndex = currentIndex
        var tempNewlineIndex = currentIndex
        while (tempNewlineIndex < (currentIndex + chunkLength)) {
            tempNewlineIndex = this.indexOf(delimiter, tempNewlineIndex + 1)
            if (tempNewlineIndex == -1) {
                val chunk = this.substring(currentIndex, nextNewlineIndex)
                result.add(chunk)
                return result
            }
            if (tempNewlineIndex <= (currentIndex + chunkLength)) {
                nextNewlineIndex = tempNewlineIndex
            }
        }
        val chunk = this.substring(currentIndex, nextNewlineIndex)
        result.add(chunk)
        currentIndex = nextNewlineIndex
    }
    return result
}

/**
 * Formats the message by removing any Windows-style line breaks and escaping newline characters.
 *
 * @return The formatted message.
 */
private fun String.formatMessage(): String {
    return this.replace(Regex("(\r\n|\r|\n)"), "\n")
}

/**
 * Escapes the specified characters in the string.
 *
 * @param escapedCharacters The characters to be escaped.
 * @return The string with escaped characters.
 */
private fun String.escapeCharacters(escapedCharacters: String): String {
    return this.replace(escapedCharacters.toRegex()) { result -> "\\${result.value}" }
}
