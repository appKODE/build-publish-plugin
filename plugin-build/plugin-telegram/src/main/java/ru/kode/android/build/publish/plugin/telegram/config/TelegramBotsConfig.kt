package ru.kode.android.build.publish.plugin.telegram.config

import groovy.lang.Closure
import groovy.lang.DelegatesTo
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import ru.kode.android.build.publish.plugin.core.util.CommonConfigMergeable
import ru.kode.android.build.publish.plugin.core.util.configureGroovy
import ru.kode.android.build.publish.plugin.core.util.inheritNamedFrom
import javax.inject.Inject

/**
 * Container class for managing multiple Telegram bot configurations.
 *
 * This class allows you to define and manage multiple bot configurations within a single project.
 * Each bot can be configured with its own settings and chat destinations.
 *
 * @see TelegramBotConfig For details on configuring individual bots
 * @see TelegramChatConfig For details on chat configuration
 */
abstract class TelegramBotsConfig
    @Inject
    constructor(
        objects: ObjectFactory,
    ) : Named,
        CommonConfigMergeable<TelegramBotsConfig> {
        /**
         * Internal container for storing bot configurations.
         *
         * This property holds all bot configurations added via the [bot] method.
         * Access to this container is restricted to the plugin implementation.
         */
        internal val bots: NamedDomainObjectContainer<TelegramBotConfig> =
            objects.domainObjectContainer(TelegramBotConfig::class.java)

        /**
         * Configures a new Telegram bot with the given name and settings.
         *
         * This method registers a new bot configuration and applies the provided
         * configuration action to it. Each bot must have a unique name within the project.
         *
         * @param botName A unique identifier for the bot configuration (e.g., "release", "dev").
         *                This name is used to reference the bot in build scripts.
         * @param action A configuration block that will be applied to a new [TelegramBotConfig] instance.
         *
         * @see TelegramBotConfig For available bot configuration options
         */
        fun bot(
            botName: String,
            action: Action<TelegramBotConfig>,
        ) {
            bots.register(botName, action)
        }

        /**
         * Configures a new Telegram bot with the given name using a Groovy closure.
         *
         * @param botName A unique identifier for the bot configuration (e.g., "release", "dev").
         * @param configurationClosure The Groovy closure applied to the new [TelegramBotConfig].
         *
         * @see bot
         */
        fun bot(
            botName: String,
            @DelegatesTo(value = TelegramBotConfig::class, strategy = Closure.DELEGATE_FIRST)
            configurationClosure: Closure<in TelegramBotConfig>,
        ) {
            bot(botName) { target -> configureGroovy(configurationClosure, target) }
        }

        override fun inheritFrom(common: TelegramBotsConfig) {
            bots.inheritNamedFrom(common.bots)
        }
    }
