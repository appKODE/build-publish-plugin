package ru.kode.android.build.publish.plugin.slack.config

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional

/**
 * Configuration class for Slack file distribution settings.
 *
 * This class defines the configuration needed to upload build artifacts (like APKs)
 * to Slack channels. It's typically used in the build script's `slack` extension
 * to configure distribution settings for different build variants.
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
    @get:Optional
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
}
