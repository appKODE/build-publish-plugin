package ru.kode.android.build.publish.plugin.slack.config

import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input

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
}
