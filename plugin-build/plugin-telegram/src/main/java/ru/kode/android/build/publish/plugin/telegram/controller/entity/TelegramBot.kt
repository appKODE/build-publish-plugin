package ru.kode.android.build.publish.plugin.telegram.controller.entity

/**
 * Represents a Telegram bot used for sending messages.
 */
@kotlinx.serialization.Serializable
internal data class TelegramBot(
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
    val serverBaseUrl: String?,
    /**
     * Optional basic authentication credentials for the Bot API server
     */
    val basicAuth: BasicAuth?,
    /**
     * List of chat configurations associated with this bot.
     */
    val chats: List<Chat>
) {
    /**
     * Represents basic authentication credentials for the Telegram Bot API server.
     */
    @kotlinx.serialization.Serializable
    internal data class BasicAuth(
        /**
         * The username for basic authentication
         */
        val username: String,
        /**
         * The password for basic authentication
         */
        val password: String,
    )

    @kotlinx.serialization.Serializable
    /**
     * Represents a chat configuration associated with a Telegram bot.
     */
    internal data class Chat(
        /**
         * The username for basic authentication
         */
        val name: String,
        /**
         * The chat ID for this chat (e.g., "-1234567890")
         */
        val id: String,
        /**
         * The topic ID for this chat.
         */
        val topicId: String?
    )
}
