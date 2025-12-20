package ru.kode.android.build.publish.plugin.telegram.controller

import ru.kode.android.build.publish.plugin.telegram.controller.entity.ChatSpecificTelegramBot
import ru.kode.android.build.publish.plugin.telegram.controller.entity.TelegramLastMessage
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
        userMentions: List<String>,
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
     * Retrieves the last message sent to the specified chat.
     *
     * @param botId The ID of the bot to use for retrieving the message
     * @param chatName The ID of the chat to retrieve the last message from
     * @param topicName The ID of the topic to retrieve the last message from; if null, retrieves the last message from the chat
     *
     * @return The last message sent to the chat, or null if no message was found
     *
     * @throws IllegalStateException If no matching bot configuration is found
     * @throws IOException If there's a network error while retrieving the message
     */
    fun getLastMessage(
        botId: String,
        chatName: String,
        topicName: String?,
    ): TelegramLastMessage?
}
