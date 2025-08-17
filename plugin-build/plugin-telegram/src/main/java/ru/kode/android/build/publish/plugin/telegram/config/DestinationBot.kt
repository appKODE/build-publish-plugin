package ru.kode.android.build.publish.plugin.telegram.config

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import javax.inject.Inject

/**
 * Configuration class that specifies which bot and chat(s) should receive notifications.
 *
 * This class is used within [TelegramChangelogConfig] to define the mapping between
 * a bot and the chat(s) that should receive changelog notifications.
 *
 * @see TelegramChangelogConfig For how to use this class in changelog configurations
 * @see TelegramBotsConfig For defining available bots
 * @see TelegramBotConfig For bot configuration details
 */
abstract class DestinationBot
    @Inject
    constructor() {
        /**
         * The name of the bot that should send the message.
         *
         * This must match the name of a bot that was configured in the [TelegramBotsConfig].
         */
        @get:Input
        abstract val botName: Property<String>

        /**
         * The names of the chats where the message should be sent.
         *
         * Each name in this set must match a chat name that was configured in the
         * specified bot.
         */
        @get:Input
        internal abstract val chatNames: SetProperty<String>

        /**
         * Adds a new chat name to the set of chat names for this destination bot.
         *
         * @param chatName The name of the chat to add.
         */
        fun chatName(chatName: String) {
            chatNames.add(chatName)
        }

        /**
         * Adds multiple chat names to the set of chat names for this destination bot.
         *
         * @param chatName The names of the chats to add.
         */
        fun chatNames(vararg chatName: String) {
            chatNames.addAll(chatName.toList())
        }
    }
