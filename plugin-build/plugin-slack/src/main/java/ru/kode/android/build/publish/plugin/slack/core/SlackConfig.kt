package ru.kode.android.build.publish.plugin.slack.core

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional

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

    /**
     * Attachment's vertical line color in hex format
     * For example: #ffffff
     */
    @get:Input
    val attachmentColor: Property<String>

    /**
     * Api token file to upload files in slack
     */
    @get:Optional
    @get:InputFile
    val uploadApiTokenFile: RegularFileProperty

    /**
     * Public channels where file will be uploaded
     */
    @get:Optional
    @get:Input
    val uploadChannels: SetProperty<String>
}
