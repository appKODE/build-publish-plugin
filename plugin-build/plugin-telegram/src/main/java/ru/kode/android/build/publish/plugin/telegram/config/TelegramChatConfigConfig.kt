package ru.kode.android.build.publish.plugin.telegram.config

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

abstract class TelegramChatConfigConfig {

    /**
     * Telegram chat id where bot is added
     */
    @get:Input
    abstract val chatId: Property<String>

    /**
     * Unique identifier for the target message thread
     * Represents "message_thread_id"
     */
    @get:Input
    @get:Optional
    abstract val topicId: Property<String>

}
