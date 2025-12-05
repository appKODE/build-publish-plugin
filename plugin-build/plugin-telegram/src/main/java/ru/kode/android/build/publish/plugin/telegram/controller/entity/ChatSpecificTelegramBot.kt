package ru.kode.android.build.publish.plugin.telegram.controller.entity

/**
 * Represents a Telegram bot used for sending messages.
 */
@kotlinx.serialization.Serializable
data class ChatSpecificTelegramBot(
    /**
     * The name of this bot configuration (automatically set from the registration name)
     */
    val name: String,
    /**
     * The bot token (e.g., "1234567890:ABC-DEF1234ghIkl-zyx57W2v1u123ew11")
     */
    val id: String,
    /**
     * The base URL for the Telegram Bot API server (defaults to "https://api.telegram.org")
     */
    val serverBaseUrl: String,
    /**
     * The unique identifier of the chat where the bot will send messages
     */
    val chatId: String,
    /**
     * Optional thread identifier for forum topics or group threads
     */
    val topicId: String?,
    /**
     * Optional basic authentication credentials for the Bot API server
     */
    val basicAuth: BasicAuth?,
) {
    /**
     * Represents basic authentication credentials for the Telegram Bot API server.
     */
    @kotlinx.serialization.Serializable
    data class BasicAuth(
        /**
         * The username for basic authentication
         */
        val username: String,
        /**
         * The password for basic authentication
         */
        val password: String,
    )
}
