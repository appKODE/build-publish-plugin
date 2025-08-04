package ru.kode.android.build.publish.plugin.slack.core

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input

interface SlackChangelogConfig {
    val name: String

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

}
