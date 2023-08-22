package ru.kode.android.build.publish.plugin.extension.config

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

interface TelegramConfig {
    val name: String

    /**
     * Telegram bot webhook url
     * For example: https://api.telegram.org/%s/sendMessage?chat_id=%s&text=%s&parse_mode=MarkdownV2
     */
    @get:Input
    val webhookUrl: Property<String>

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
     */
    @get:Input
    @get:Optional
    val topicId: Property<String>

    /**
     * List of mentioning users for Slack, can be empty or null
     * For example: ["@aa", "@bb", "@ccc"]
     */
    @get:Input
    val userMentions: SetProperty<String>
}
