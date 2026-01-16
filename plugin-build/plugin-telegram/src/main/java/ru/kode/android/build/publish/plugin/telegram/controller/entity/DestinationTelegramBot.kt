package ru.kode.android.build.publish.plugin.telegram.controller.entity

/**
 * Represents a destination bot with a bot name and a list of chat names.
 *
 * @property botName The name of the bot.
 * @property chatNames The names of the chats.
 */
@kotlinx.serialization.Serializable
data class DestinationTelegramBot(
    /**
     * The name of the bot.
     */
    val botName: String,
    /**
     * The names of the chats.
     */
    val chatNames: List<String>,
)
