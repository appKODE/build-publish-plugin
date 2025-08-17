package ru.kode.android.build.publish.plugin.slack.config

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input

/**
 * Configuration class for Slack changelog notification settings.
 *
 * This class defines the configuration needed to send changelog notifications
 * to Slack channels. It's typically used in the build script's `slack` extension
 * to configure changelog notifications for different build variants.
 */
abstract class SlackChangelogConfig {
    abstract val name: String

    /**
     * Set of Slack user or group mentions to be included in the changelog notification.
     *
     * Examples:
     * - `@username` - Mentions a specific user
     * - `@here` - Notifies all active users in the channel
     * - `@channel` - Notifies all users in the channel
     * - `@group-name` - Mentions a user group
     *
     * The actual mentions will be formatted as Slack mentions in the notification message.
     */
    @get:Input
    internal abstract val userMentions: SetProperty<String>

    /**
     * The color of the vertical line in the Slack message attachment.
     *
     * This should be a valid hex color code (e.g., "#36a64f" for green).
     * The color helps visually distinguish different types of notifications.
     *
     * Common colors:
     * - `#36a64f` - Green (success)
     * - `#3aa3e3` - Blue (info)
     * - `#ffcc4d` - Yellow (warning)
     * - `#e01e5a` - Red (error)
     */
    @get:Input
    abstract val attachmentColor: Property<String>

    /**
     * Adds a single user or group mention to the changelog notification.
     *
     * @param userMention The Slack mention string (e.g., "@username", "@here", "@group-name")
     */
    fun userMention(userMention: String) {
        userMentions.add(userMention)
    }

    /**
     * Adds multiple user or group mentions to the changelog notification.
     *
     * @param userMention Vararg of Slack mention strings (e.g., "@username", "@here", "@group-name")
     */
    fun userMentions(vararg userMention: String) {
        userMentions.addAll(userMention.toList())
    }
}
