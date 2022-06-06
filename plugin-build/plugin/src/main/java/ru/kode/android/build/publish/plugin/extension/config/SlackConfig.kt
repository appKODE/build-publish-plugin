package ru.kode.android.build.publish.plugin.extension.config

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input

interface SlackConfig {
    val name: String

    /**
     * Slack bot webhook url
     * For example: https://hooks.slack.com/services/111111111/AAAAAAA/DDDDDDD
     */
    @get:Input
    val webhookUrl: Property<String>

    /**
     * Slack bot icon url
     * For example: https://i.imgur.com/HQTF5FK.png
     */
    @get:Input
    val iconUrl: Property<String>

    /**
     * List of mentioning users for Slack, can be empty or null
     * For example: ["@aa", "@bb", "@ccc"]
     */
    @get:Input
    val userMentions: SetProperty<String>
}
