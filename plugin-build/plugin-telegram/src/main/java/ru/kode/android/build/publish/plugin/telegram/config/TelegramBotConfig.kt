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
 * Abstract configuration for a Telegram bot used for posting changelogs or other messages.
 *
 * This class provides properties to configure the bot's ID, server URL, authorization,
 * and manages a collection of chats where the bot has been added.
 *
 * @constructor Injects the [ObjectFactory] used for creating nested configuration objects.
 */
abstract class TelegramBotConfig @Inject constructor(
    objects: ObjectFactory
) : Named {

    /**
     * Telegram bot token (bot ID) used for authenticating API requests to post messages.
     */
    @get:Input
    abstract val botId: Property<String>

    /**
     * Optional base URL of the Telegram bot API server.
     * Defaults to "api.telegram.org" if not provided.
     */
    @get:Input
    @get:Optional
    abstract val botServerBaseUrl: Property<String>

    /**
     * Optional basic authentication credentials for the Telegram bot server.
     * If not provided, no authentication header is applied.
     */
    @get:Nested
    @get:Optional
    val botServerAuth: BasicAuthCredentials =
        objects.newInstance(BasicAuthCredentials::class.java)

    /**
     * Internal container of [TelegramChatConfig] objects representing the chats
     * where this bot has been added.
     */
    internal val chats: NamedDomainObjectContainer<TelegramChatConfig> =
        objects.domainObjectContainer(TelegramChatConfig::class.java)

    /**
     * Registers a new chat configuration in the list of chats where this bot is added.
     *
     * @param chatName A unique identifier for the chat configuration,
     *                  e.g. a descriptive name like "builds" or "alerts".
     * @param action Configuration action to apply to the [TelegramChatConfig].
     */
    fun chat(chatName: String, action: Action<TelegramChatConfig>) {
        chats.register(chatName, action)
    }
}
