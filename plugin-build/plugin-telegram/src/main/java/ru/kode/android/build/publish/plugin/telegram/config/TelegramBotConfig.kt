package ru.kode.android.build.publish.plugin.telegram.config

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import ru.kode.android.build.publish.plugin.core.api.config.BasicAuthCredentials
import javax.inject.Inject

/**
 * Configuration class for a Telegram bot used for sending build notifications and changelogs.
 *
 * This class allows you to configure the connection to a Telegram bot and define the chats
 * where notifications should be sent. It supports both standard and self-hosted Telegram Bot API servers.
 *
 * @see TelegramChatConfig For configuring individual chat destinations
 * @see BasicAuthCredentials For authentication configuration
 */
abstract class TelegramBotConfig
    @Inject
    constructor(
        objects: ObjectFactory,
    ) : Named {
        /**
         * The Telegram bot token used for authenticating API requests.
         *
         * This is a required property that identifies your bot when making requests to the Telegram Bot API.
         * You can obtain this token by talking to [@BotFather](https://t.me/botfather) on Telegram.
         *
         * Example: `"1234567890:ABC-DEF1234ghIkl-zyx57W2v1u123ew11"`
         */
        @get:Input
        abstract val botId: Property<String>

        /**
         * The base URL of the Telegram Bot API server.
         *
         * This is an optional property that defaults to Telegram's official API endpoint.
         * You only need to set this if you're using a self-hosted Bot API server.
         *
         * Default: `"https://api.telegram.org"`
         */
        @get:Input
        @get:Optional
        abstract val botServerBaseUrl: Property<String>

        /**
         * Basic authentication credentials for the Telegram Bot API server.
         *
         * This is only required if your self-hosted Bot API server is protected with HTTP Basic Auth.
         * Leave this unconfigured when using the official Telegram Bot API.
         *
         * Example:
         * ```groovy
         * botServerAuth {
         *     username = providers.environmentVariable("TELEGRAM_AUTH_USER")
         *     password = providers.environmentVariable("TELEGRAM_AUTH_PASSWORD")
         * }
         * ```
         */
        @get:Nested
        @get:Optional
        val botServerAuth: BasicAuthCredentials =
            objects.newInstance(BasicAuthCredentials::class.java)

        /**
         * Internal container of chat configurations where this bot will send messages.
         *
         * This is an internal property that holds all chat configurations added via the [chat] method.
         * Use the [chat] method to add new chat configurations instead of accessing this directly.
         */
        internal val chats: NamedDomainObjectContainer<TelegramChatConfig> =
            objects.domainObjectContainer(TelegramChatConfig::class.java)

        /**
         * Configures a chat where the bot will send notifications.
         *
         * This method registers a new chat configuration with the given name and applies the provided
         * configuration action. Each chat configuration must have a unique name within the bot.
         *
         * @param chatName A unique identifier for the chat configuration (e.g., "releases", "builds", "alerts").
         *                 This is used to reference the chat in build scripts and should be descriptive.
         * @param action A configuration block that will be applied to a new [TelegramChatConfig] instance.
         *
         * @see TelegramChatConfig For available chat configuration options
         */
        fun chat(
            chatName: String,
            action: Action<TelegramChatConfig>,
        ) {
            chats.register(chatName, action)
        }
    }
