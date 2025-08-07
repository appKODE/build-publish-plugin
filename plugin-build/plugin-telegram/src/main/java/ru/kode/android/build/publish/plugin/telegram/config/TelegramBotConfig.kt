package ru.kode.android.build.publish.plugin.telegram.config

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

interface TelegramBotConfig {
    val name: String

    /**
     * Telegram bot id to post changelog in chat
     */
    @get:Input
    val botId: Property<String>

    /**
     * Telegram chat id where changelog will be posted
     */
    @get:Input
    val chatId: Property<String>

    /**
     * Unique identifier for the target message thread
     * Represents "message_thread_id"
     */
    @get:Input
    @get:Optional
    val topicId: Property<String>
}
