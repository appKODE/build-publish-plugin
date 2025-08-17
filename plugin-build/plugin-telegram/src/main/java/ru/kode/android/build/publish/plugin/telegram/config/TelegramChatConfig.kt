package ru.kode.android.build.publish.plugin.telegram.config

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

/**
 * Configuration for a Telegram chat where the bot will send messages.
 *
 * This class represents a single chat or chat thread where notifications will be sent.
 * Each chat configuration must have a unique name within its parent bot configuration.
 *
 * @see TelegramBotConfig For how to register chat configurations
 */
abstract class TelegramChatConfig {
    abstract val name: String

    /**
     * The unique identifier of the chat where the bot will send messages.
     *
     * This is a required property that specifies which chat the bot should post to.
     * The chat ID can be a user ID, group ID, or channel username (with @).
     *
     * Example values:
     * - `"@channelusername"` (for public channels)
     * - `"-1001234567890"` (for private groups/channels)
     * - `"123456789"` (for direct messages to a user)
     */
    @get:Input
    abstract val chatId: Property<String>

    /**
     * The unique identifier for the target message thread (topic) in a forum or group.
     *
     * This is an optional property that allows sending messages to a specific thread
     * in a forum or group chat. If not specified, messages will be sent to the main chat.
     *
     * Note: The bot must be a member of the group and have permission to post in the thread.
     */
    @get:Input
    @get:Optional
    abstract val topicId: Property<String>
}
