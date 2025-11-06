package ru.kode.android.build.publish.plugin.slack.config

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional

/**
 * Configuration class for Slack file distribution settings with changelog support.
 *
 * This class defines the configuration needed to upload build artifacts (like APKs)
 * to Slack channels with changelog messages. It's typically used in the
 * build script's `slack` extension to configure distribution settings for different build variants.
 *
 * @see <a href="https://docs.slack.dev/reference/block-kit/blocks/rich-text-block/ ">changelog: Rich text block</a>
 */
abstract class SlackDistributionConfig {
    abstract val name: String

    /**
     * File containing the Slack API token with `files:write` scope.
     *
     * The token should have the following OAuth scopes:
     * - `files:write` - To upload files
     * - `channels:read` - To verify channel access
     * - `groups:read` - For private channel access
     *
     * @see <a href="https://api.slack.com/authentication/token-types#bot">Slack Bot Tokens</a>
     */
    @get:InputFile
    abstract val uploadApiTokenFile: RegularFileProperty

    /**
     * Set of Slack channel IDs where the build artifacts will be uploaded.
     *
     * Channel IDs should be in the format `"#channel-name"` for public channels
     * or `"#private-channel-name"` for private groups.
     */
    @get:Input
    internal abstract val destinationChannels: SetProperty<String>

    /**
     * Adds a single destination channel for file uploads.
     *
     * @param channelId The Slack channel ID (e.g., "#releases") where files should be uploaded
     */
    fun destinationChannel(channelId: String) {
        destinationChannels.add(channelId)
    }

    /**
     * Adds multiple destination channels for file uploads.
     *
     * @param channelId Vararg of Slack channel IDs (e.g., "#releases", "#android-team")
     */
    fun destinationChannels(vararg channelId: String) {
        destinationChannels.addAll(channelId.toList())
    }

    /**
     * Set of Slack user mentions to be included in the changelog message.
     *
     * Examples:
     * - `@username` - Mentions a specific user
     * - `@here` - Notifies all active users in the channel
     * - `@channel` - Notifies all users in the channel
     * - `@group-name` - Mentions a user group
     */
    @get:Optional
    @get:Input
    abstract val userMentions: SetProperty<String>

    /**
     * Version string to display in the changelog message.
     * This will be shown as a separate line in the rich text message.
     */
    @get:Optional
    @get:Input
    abstract val distributionDescription: Property<String>

    /**
     * Adds a single user mention to the changelog message.
     *
     * @param userMention The Slack mention string (e.g., "@username", "@here", "@group-name")
     */
    fun userMention(userMention: String) {
        userMentions.add(userMention)
    }

    /**
     * Adds multiple user mentions to the changelog message.
     *
     * @param userMention Vararg of Slack mention strings (e.g., "@username", "@here", "@group-name")
     */
    fun userMentions(vararg userMention: String) {
        userMentions.addAll(userMention.toList())
    }

    /**
     * Sets the distribution Description string to display in the changelog message.
     *
     * @param value The version string (e.g., "RC v1.3.1990", "v2.0.0", "NO QA", "SOME FEATURE TEST", "DO NOT PUBLISH")
     */
    fun distributionDescription(value: String) {
        distributionDescription.set(value)
    }
}
