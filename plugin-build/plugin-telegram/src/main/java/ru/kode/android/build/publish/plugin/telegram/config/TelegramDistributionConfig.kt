package ru.kode.android.build.publish.plugin.telegram.config

import groovy.lang.Closure
import groovy.lang.DelegatesTo
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import ru.kode.android.build.publish.plugin.core.util.CollectionStrategy
import ru.kode.android.build.publish.plugin.core.util.CommonConfigMergeable
import ru.kode.android.build.publish.plugin.core.util.configureGroovy
import ru.kode.android.build.publish.plugin.core.util.inheritFrom
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
    ) : CommonConfigMergeable<TelegramDistributionConfig> {
        /**
         * Name of this distribution configuration.
         *
         * Used to match a configuration to a build variant (or to the common configuration).
         */
        abstract val name: String

        /**
         * Whether to compress the distribution file before sending.
         *
         * When set to `true`, the file will be compressed before being sent to Telegram.
         * This can help reduce upload time for large files.
         *
         * Default: `false`
         */
        @get:Input
        @get:Optional
        abstract val compressed: Property<Boolean>

        /**
         * Internal set of destination bot configurations.
         *
         * This property holds the list of bot and chat combinations that should
         * receive distribution notifications. Use the [destinationBot] method to
         * add new destinations instead of modifying this directly.
         */
        @get:Input
        internal abstract val destinationBots: SetProperty<DestinationTelegramBotConfig>

        private var destinationBotsStrategy: CollectionStrategy = CollectionStrategy.REPLACE

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

        override fun inheritFrom(common: TelegramDistributionConfig) {
            compressed.convention(common.compressed)
            destinationBots.inheritFrom(common.destinationBots, destinationBotsStrategy)
        }
    }
