package ru.kode.android.build.publish.plugin.telegram.controller

import ru.kode.android.build.publish.plugin.telegram.controller.entity.ChatSpecificTelegramBot
import ru.kode.android.build.publish.plugin.telegram.controller.entity.TelegramLastMessage
import ru.kode.android.build.publish.plugin.telegram.network.entity.TelegramMessage
import java.io.File

/**
 * The TelegramController interface defines methods for sending messages to Telegram chats.
 *
 * The TelegramController interface provides methods for sending messages to Telegram chats
 * using the configured Telegram bots. It allows to send both text and file messages.
 */
interface TelegramController {

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
    fun send(
        message: String,
        header: String,
        userMentions: List<String>?,
        issueUrlPrefix: String,
        issueNumberPattern: String,
        bots: List<ChatSpecificTelegramBot>,
    )

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
     * @see send For sending text messages without file attachments
     */
    fun upload(
        file: File,
        bots: List<ChatSpecificTelegramBot>,
    )

    /**
     * Retrieves the last message from the specified Telegram chat.
     *
     * This method fetches the last message from the specified Telegram chat.
     *
     * @param bot The [ChatSpecificTelegramBot] configuration.
     * @return The last [TelegramMessage] if available, otherwise null.
     */
    fun getLastMessage(bot: ChatSpecificTelegramBot): TelegramLastMessage?

}
