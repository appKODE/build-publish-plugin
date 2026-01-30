package ru.kode.android.build.publish.plugin.telegram.controller

import okhttp3.Credentials
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import ru.kode.android.build.publish.plugin.core.logger.PluginLogger
import ru.kode.android.build.publish.plugin.core.util.createPartFromString
import ru.kode.android.build.publish.plugin.core.util.executeNoResult
import ru.kode.android.build.publish.plugin.core.util.executeWithResult
import ru.kode.android.build.publish.plugin.telegram.controller.entity.ChatSpecificTelegramBot
import ru.kode.android.build.publish.plugin.telegram.controller.entity.TelegramLastMessage
import ru.kode.android.build.publish.plugin.telegram.messages.sendingMessageBotMessage
import ru.kode.android.build.publish.plugin.telegram.messages.uploadFileStartedMessage
import ru.kode.android.build.publish.plugin.telegram.network.api.TelegramDistributionApi
import ru.kode.android.build.publish.plugin.telegram.network.api.TelegramWebhookApi
import ru.kode.android.build.publish.plugin.telegram.network.entity.SendMessageRequest
import ru.kode.android.build.publish.plugin.telegram.network.entity.TelegramMessage
import java.io.File

internal const val TELEGRAM_DEFAULT_BASE_RUL = "https://api.telegram.org"
private const val SEND_MESSAGE_TO_CHAT_WEB_HOOK = "%s/bot%s/sendMessage"
private const val GET_MESSAGE_IN_CHAT_WEB_HOOK = "%s/bot%s/getUpdates"
private const val SEND_DOCUMENT_WEB_HOOK = "%s/bot%s/sendDocument"
private const val MESSAGE_MAX_LENGTH = 4096

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
    private val logger: PluginLogger,
) : TelegramController {
    /**
     * Sends a text message to the specified Telegram chats using the configured bots in chunks.
     *
     * This method sends an HTML-formatted message to one or more Telegram chats
     * using the specified bots. The message will be split into chunks if it exceeds the maximum
     * length allowed by Telegram. The chunks will be separated by newlines.
     *
     * @param message The message body to send
     * @param header Header text shown at the top of the message
     * @param userMentions User mentions to include at the top of the message
     * @param issueUrlPrefix URL prefix used to build links to issues
     * @param issueNumberPattern Regex pattern used to find issue keys inside the message
     * @param bots List of bot+chat destinations
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
        val messageWithIssuesLinks = formatIssuesHtml(message, issueUrlPrefix, issueNumberPattern)
        val htmlMessage = buildHtmlMessage(header, userMentions, messageWithIssuesLinks)

        val chunks = htmlMessage.chunkedByLines(MESSAGE_MAX_LENGTH)
        chunks.forEach { chunk ->
            sendMessage(chunk, bots)
        }
    }

    /**
     * Uploads a file to the specified Telegram chats using the configured bots.
     *
     * This method sends a file to one or more Telegram chats using the specified bots.
     * The file will be sent as a document, and a caption can be included.
     *
     * @param file The file to upload
     * @param bots List of bot+chat destinations
     *
     * @throws IllegalStateException If no matching bot configuration is found
     * @throws IOException If there's a network error or the file cannot be read
     *
     * @see send For sending text messages without file attachments
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
     * @param botId The ID of the bot to use for retrieving the message.
     * @param chatName The name of the chat to retrieve the last message from.
     * @param topicName The name of the topic to retrieve the last message from; if null, retrieves the last message from the chat.
     * @return The last [TelegramMessage] if available, otherwise null.
     */
    override fun getLastMessage(
        botId: String,
        chatName: String,
        topicName: String?,
    ): TelegramLastMessage? {
        val webhookUrl =
            GET_MESSAGE_IN_CHAT_WEB_HOOK.format(
                TELEGRAM_DEFAULT_BASE_RUL,
                botId,
            )

        val response =
            webhookApi
                .getUpdates(null, webhookUrl)
                .executeWithResult()
                .getOrThrow()

        val messages =
            response
                .result
                .mapNotNull { it.channel_post ?: it.edited_message ?: it.message }
                .filter { it.chat.title.contains(chatName, ignoreCase = true) }

        val messageWithTopic =
            messages.firstOrNull {
                val messageTopicName =
                    it.forum_topic_created?.name
                        ?: it.reply_to_message?.forum_topic_created?.name
                it.message_thread_id != null &&
                    messageTopicName != null &&
                    topicName != null &&
                    messageTopicName.contains(topicName, ignoreCase = true) &&
                    !it.text.isNullOrBlank()
            }
        val lastMessage =
            messages
                .sortedByDescending { it.date }
                .firstOrNull { !it.text.isNullOrBlank() }

        val topicId = messageWithTopic?.message_thread_id
        return if (topicId != null && topicName != null) {
            TelegramLastMessage(
                text = messageWithTopic.text,
                chatName = messageWithTopic.chat.title,
                chatId = messageWithTopic.chat.id.toString(),
                topicId = topicId.toString(),
                topicName = lastMessage?.forum_topic_created?.name ?: topicName,
            )
        } else if (topicName == null && lastMessage != null) {
            TelegramLastMessage(
                text = lastMessage.text,
                chatId = lastMessage.chat.id.toString(),
                topicId = null,
                topicName = null,
                chatName = lastMessage.chat.title,
            )
        } else {
            null
        }
    }

    /**
     * Sends a message to each chat specified in the [bots] using the corresponding bot configuration.
     *
     * @param message The message to send to the chats.
     * @param bots The list of [ChatSpecificTelegramBot] configurations specifying the chats and bot settings.
     */
    private fun sendMessage(
        message: String,
        bots: List<ChatSpecificTelegramBot>,
    ) {
        bots.forEach { bot ->
            val webhookUrl =
                SEND_MESSAGE_TO_CHAT_WEB_HOOK.format(
                    bot.serverBaseUrl
                        ?: TELEGRAM_DEFAULT_BASE_RUL,
                    bot.id,
                )

            logger.info(sendingMessageBotMessage(bot.name, webhookUrl))
            val authorization =
                bot.basicAuth?.let {
                    Credentials.basic(it.username, it.password)
                }

            webhookApi.send(
                authorization,
                webhookUrl,
                SendMessageRequest(
                    chat_id = bot.chatId,
                    message_thread_id = bot.topicId,
                    text = message,
                    parse_mode = "HTML",
                    disable_web_page_preview = true,
                ),
            ).executeNoResult().getOrThrow()
        }
    }
}

/**
 * Builds an HTML message with the given [header], [userMentions] and [body].
 *
 * @param header The header of the message.
 * @param userMentions The list of user mentions.
 * @param body The body of the message.
 * @return The HTML message.
 */
private fun buildHtmlMessage(
    header: String,
    userMentions: List<String>,
    body: String,
): String {
    val escapedHeader = escapeHtml(header)
    val mentions = userMentions.joinToString(", ") { escapeHtml(it) }
    return buildString {
        appendLine("<b>$escapedHeader</b>")
        appendLine(mentions)
        appendLine()
        body.lines().forEach { line ->
            appendLine(line.trim())
        }
    }
}

/**
 * Formats the given [message] by replacing all occurrences of issue numbers with hyperlinks to the
 * specified [issueUrlPrefix].
 *
 * @param message The message to format.
 * @param issueUrlPrefix The prefix URL for the issue tracker.
 * @param issueNumberPattern The regular expression pattern to match issue numbers.
 * @return The formatted message with issue numbers replaced with hyperlinks.
 */
private fun formatIssuesHtml(
    message: String,
    issueUrlPrefix: String,
    issueNumberPattern: String,
): String {
    val issueRegexp = issueNumberPattern.toRegex()
    var formattedMessage = markdownToTelegramHtml(message)

    issueRegexp.findAll(message).distinctBy { it.value }.forEach { match ->
        val issueKey = match.value
        val link = "<a href=\"$issueUrlPrefix$issueKey\">$issueKey</a>"
        // Replace issue with formatted HTML link
        formattedMessage = formattedMessage.replace(issueKey, link)
    }
    return formattedMessage
}

/**
 * Escapes special characters in the given [input] string to their HTML entities.
 *
 * @param input The input string to escape.
 * @return The escaped string.
 */
private fun escapeHtml(input: String): String {
    return input
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
}

/**
 * Splits the string into lines of maximum [maxLength] characters.
 *
 * @param maxLength the maximum length of each line, excluding line breaks.
 * @return a list of strings, each of which is less than or equal to [maxLength] characters.
 */
private fun String.chunkedByLines(maxLength: Int): List<String> {
    val result = mutableListOf<String>()
    val lines = this.lines()
    var buffer = StringBuilder()
    for (line in lines) {
        if (buffer.length + line.length + 1 > maxLength) {
            result += buffer.toString()
            buffer = StringBuilder()
        }
        if (buffer.isNotEmpty()) buffer.append("\n")
        buffer.append(line)
    }
    if (buffer.isNotEmpty()) result += buffer.toString()
    return result
}

/**
 * Converts Markdown-formatted text to Telegram-compatible HTML.
 *
 * This function handles the following Markdown elements:
 * - Fenced code blocks (```) → `<pre><code>...</code></pre>`
 * - Inline code (`) → `<code>...</code>`
 * - Bold (**text** or __text__) → `<b>...</b>`
 * - Italic (*text* or _text_) → `<i>...</i>`
 * - Strikethrough (~~text~~) → `<s>...</s>`
 * - Links ([text](url)) → `<a href="url">text</a>`
 *
 * Special HTML characters are escaped to prevent injection issues.
 *
 * @param input The Markdown-formatted input string.
 * @return The Telegram-compatible HTML string.
 */
private fun markdownToTelegramHtml(input: String): String {
    val codeBlocks = mutableListOf<String>()
    var text =
        input.replace(Regex("```([\\s\\S]*?)```")) { m ->
            val raw = m.groupValues[1].trim('\n', '\r')
            val escaped = escapeHtml(raw)
            val html = "<pre><code>$escaped</code></pre>"
            val idx = codeBlocks.size
            codeBlocks += html
            "%%CODEBLOCK_$idx%%"
        }

    text = escapeHtml(text)

    text =
        text.replace(Regex("`([^`\\n]+)`")) { m ->
            "<code>${m.groupValues[1]}</code>" // already escaped
        }

    text =
        text.replace(Regex("\\[([^\\]]+)]\\(([^)\\s]+)\\)")) { m ->
            val label = m.groupValues[1]
            val url = m.groupValues[2]
            "<a href=\"$url\">$label</a>"
        }

    text =
        text.replace(Regex("\\*\\*([^*\\n]+)\\*\\*")) { m ->
            "<b>${m.groupValues[1]}</b>"
        }

    text =
        text.replace(Regex("(?<!\\*)\\*([^*\\n]+)\\*(?!\\*)")) { m ->
            "<i>${m.groupValues[1]}</i>"
        }
    text =
        text.replace(Regex("_(\\S[^_\\n]*\\S)_")) { m ->
            "<i>${m.groupValues[1]}</i>"
        }

    text =
        text.replace(Regex("~~([^~\\n]+)~~")) { m ->
            "<s>${m.groupValues[1]}</s>"
        }

    codeBlocks.forEachIndexed { i, html ->
        text = text.replace("%%CODEBLOCK_$i%%", html)
    }

    return text
}
