package ru.kode.android.build.publish.plugin.telegram.config

import groovy.lang.Closure
import groovy.lang.DelegatesTo
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import ru.kode.android.build.publish.plugin.core.util.CollectionStrategy
import ru.kode.android.build.publish.plugin.core.util.CommonConfigMergeable
import ru.kode.android.build.publish.plugin.core.util.configureGroovy
import ru.kode.android.build.publish.plugin.core.util.inheritFrom
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
    ) : CommonConfigMergeable<TelegramChangelogConfig> {
        /**
         * Name of this changelog configuration.
         *
         * Used to match a configuration to a build variant (or to the common configuration).
         */
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

        private var userMentionsStrategy: CollectionStrategy = CollectionStrategy.REPLACE

        private var destinationBotsStrategy: CollectionStrategy = CollectionStrategy.REPLACE

        /**
         * Adds a single user mention to the list of user mentions.
         *
         * @param userMention The Telegram username to mention. It should include the '@' prefix.
         */
        fun userMention(userMention: String) {
            userMentions.add(userMention)
        }

        /**
         * Adds a single user mention to the list of user mentions.
         *
         * @param userMention The [Provider] of the Telegram username to mention.
         * It should include the '@' prefix.
         */
        fun userMention(userMention: Provider<String>) {
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
         * Adds multiple user mentions and selects how this collection is merged with the common
         * configuration for a per-version-name build ([CollectionStrategy.REPLACE] by default).
         *
         * @param strategy Whether to replace or append to the common mentions
         * @param userMention The array of Telegram usernames to mention (each including '@').
         */
        fun userMentions(
            strategy: CollectionStrategy,
            vararg userMention: String,
        ) {
            userMentionsStrategy = strategy
            userMentions.addAll(userMention.toList())
        }

        /**
         * Adds multiple user mentions to the list of user mentions.
         *
         * @param userMentions The collection of Telegram usernames to mention.
         * Each username should include the '@' prefix.
         **/
        fun userMentions(userMentions: Iterable<String>) {
            this.userMentions.addAll(userMentions)
        }

        /**
         * Adds multiple user mentions to the list of user mentions.
         *
         * @param userMentions Provider of collection of Telegram usernames to mention.
         * Each username should include the '@' prefix.
         *
         * Note: The [Provider] is used to enable lazy evaluation of the user mentions.
         * If the provider is not used, the user mentions will be eagerly resolved.
         **/
        fun userMentions(userMentions: Provider<List<String>>) {
            this.userMentions.addAll(userMentions)
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

        /**
         * Adds a destination bot using a Groovy closure.
         *
         * @param configurationClosure The Groovy closure applied to a new [DestinationTelegramBotConfig].
         *
         * @see destinationBot
         */
        fun destinationBot(
            @DelegatesTo(value = DestinationTelegramBotConfig::class, strategy = Closure.DELEGATE_FIRST)
            configurationClosure: Closure<in DestinationTelegramBotConfig>,
        ) {
            destinationBot { target -> configureGroovy(configurationClosure, target) }
        }

        /**
         * Adds a destination bot and selects how the destination bots collection is merged with the
         * common configuration for a per-version-name build ([CollectionStrategy.REPLACE] by default).
         *
         * @param strategy Whether to replace or append to the common destination bots
         * @param action A configuration block applied to a new [DestinationTelegramBotConfig] instance.
         */
        fun destinationBot(
            strategy: CollectionStrategy,
            action: Action<DestinationTelegramBotConfig>,
        ) {
            destinationBotsStrategy = strategy
            destinationBot(action)
        }

        override fun inheritFrom(common: TelegramChangelogConfig) {
            userMentions.inheritFrom(common.userMentions, userMentionsStrategy)
            destinationBots.inheritFrom(common.destinationBots, destinationBotsStrategy)
        }
    }
