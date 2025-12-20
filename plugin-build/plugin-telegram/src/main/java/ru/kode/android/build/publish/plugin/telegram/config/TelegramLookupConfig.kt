package ru.kode.android.build.publish.plugin.telegram.config

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

/**
 * Represents the common Telegram lookup configuration.
 *
 * This class defines the configuration properties that are common to all
 * build variants.
 */
abstract class TelegramLookupConfig {
    abstract val name: String

    /**
     * The name of the bot that should send the message.
     *
     * This must match the name of a bot that was configured in the [TelegramBotsConfig].
     */
    @get:Input
    abstract val botName: Property<String>

    /**
     * The name of the chat where the message should be sent.
     *
     * This must match the name of a chat that from Telegram.
     */
    @get:Input
    abstract val chatName: Property<String>

    /**
     * The name of the topic in the chat where the message should be sent.
     *
     * This is an optional property that allows to filter messages by topic.
     * If specified, only messages that reference the specified topic will be considered.
     *
     * If not specified, all messages in the chat will be considered, regardless of the topic.
     */
    @get:Input
    @get:Optional
    abstract val topicName: Property<String>
}
