package ru.kode.android.build.publish.plugin.telegram.config

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import javax.inject.Inject

/**
 * Configuration for sending changelog notifications to Telegram.
 *
 * This class allows you to configure how changelog messages are sent to Telegram,
 * including user mentions and destination bot configurations.
 *
 * @see TelegramBotConfig For bot configuration details
 * @see DestinationTelegramBotConfig For destination bot configuration
 */
abstract class TelegramChangelogConfig
    @Inject
    constructor(
        private val objects: ObjectFactory,
    ) {
        abstract val name: String

        /**
         * Set of Telegram usernames to mention in changelog messages.
         *
         * These usernames will be included at the beginning of the changelog message
         * to notify specific users about the update. Each username should include
         * the '@' prefix.
         *
         * Example:
         * ```groovy
         * userMentions = ["@dev1", "@qa_team", "@product_owner"]
         * ```
         *
         * The resulting message will start with:
         * "@dev1 @qa_team @product_owner New update available! ..."
         */
        @get:Input
        internal abstract val userMentions: SetProperty<String>

        /**
         * Internal set of destination bot configurations.
         *
         * This property holds the list of bot and chat combinations that should
         * receive changelog notifications. Use the [destinationBot] method to
         * add new destinations instead of modifying this directly.
         */
        @get:Input
        internal abstract val destinationBots: SetProperty<DestinationTelegramBotConfig>

        /**
         * Adds a single user mention to the list of user mentions.
         *
         * @param userMention The Telegram username to mention. It should include the '@' prefix.
         */
        fun userMention(userMention: String) {
            userMentions.add(userMention)
        }

        /**
         * Adds multiple user mentions to the list of user mentions.
         *
         * @param userMention The array of Telegram usernames to mention.
         * Each username should include the '@' prefix.
         **/
        fun userMentions(vararg userMention: String) {
            userMentions.addAll(userMention.toList())
        }

        /**
         * Configures a destination bot for sending changelog notifications.
         *
         * This method creates a new [DestinationTelegramBotConfig] configuration and applies
         * the provided action to it. The configured bot and chat must exist
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
