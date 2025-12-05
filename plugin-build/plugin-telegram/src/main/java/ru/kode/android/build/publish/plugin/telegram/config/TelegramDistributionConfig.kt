package ru.kode.android.build.publish.plugin.telegram.config

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import javax.inject.Inject

/**
 * Configuration for sending distribution notifications to Telegram.
 *
 * This class allows you to configure how distribution notifications (e.g., new app versions)
 * are sent to Telegram. It supports sending notifications to multiple bots and chats.
 *
 * @see DestinationTelegramBotConfig For details on configuring destination bots
 * @see TelegramBotsConfig For defining available bots
 */
abstract class TelegramDistributionConfig
    @Inject
    constructor(
        private val objects: ObjectFactory,
    ) {
        abstract val name: String

        /**
         * Internal set of destination bot configurations.
         *
         * This property holds the list of bot and chat combinations that should
         * receive distribution notifications. Use the [destinationBot] method to
         * add new destinations instead of modifying this directly.
         */
        @get:Input
        internal abstract val destinationBots: SetProperty<DestinationTelegramBotConfig>

        /**
         * Configures a destination bot for sending distribution notifications.
         *
         * This method creates a new [DestinationTelegramBotConfig] configuration and applies
         * the provided action to it. The configured bot and chats must exist
         * in the main Telegram configuration.
         *
         * @param action A configuration block that will be applied to a new [DestinationTelegramBotConfig] instance.
         *
         * @see DestinationTelegramBotConfig For available configuration options
         */
        fun destinationBot(action: Action<DestinationTelegramBotConfig>) {
            val destinationBot = objects.newInstance(DestinationTelegramBotConfig::class.java)
            action.execute(destinationBot)
            destinationBots.add(destinationBot)
        }
    }
