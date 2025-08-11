package ru.kode.android.build.publish.plugin.extension.config

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

interface TelegramConfig {
    val name: String

    /**
     * Telegram bot id to post changelog in chat
     */
    @get:Input
    val botId: Property<String>

    /**
     * Bot server base url
     */
    @get:Input
    @get:Optional
    val botBaseUrl: Property<String>

    /**
     * Bot server auth username
     */
    @get:Input
    @get:Optional
    val botAuthUsername: Property<String>

    /**
     * Bot server auth password
     */
    @get:Input
    @get:Optional
    val botAuthPassword: Property<String>

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

    /**
     * List of mentioning users for Slack, can be empty or null
     * For example: ["@aa", "@bb", "@ccc"]
     */
    @get:Input
    val userMentions: SetProperty<String>

    /**
     * Should upload build at the same chat or not
     * Works only if file size is smaller then 50 mb
     */
    @get:Input
    @get:Optional
    val uploadBuild: Property<Boolean>
}
