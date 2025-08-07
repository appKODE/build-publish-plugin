package ru.kode.android.build.publish.plugin.slack.core

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

interface SlackBotConfig {
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
}
