package ru.kode.android.build.publish.plugin.play.config

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

/**
 * Configuration interface for Google Play distribution settings.
 *
 * This interface defines the distribution parameters for publishing an Android app
 * to the Google Play Store. It's used within the `BuildPublishPlayExtension` to
 * configure how the app should be published to different tracks.
 */
interface PlayDistributionConfig {
    /**
     * Name of this distribution configuration.
     *
     * Used to match a configuration to a build variant (or to the common configuration).
     */
    val name: String

    /**
     * The target track for publishing the app.
     *
     * Common track values include:
     * - `"internal"` - For internal testing (default)
     * - `"alpha"` - For alpha testing
     * - `"beta"` - For beta testing
     * - `"production"` - For production release
     * - `"rollout"` - For staged rollouts
     *
     * Default value: `"internal"`
     */
    @get:Input
    val trackId: Property<String>

    /**
     * The priority of the update for the app.
     *
     * This is the in-app update priority passed to Google Play.
     * Valid values are in the range `0..5`.
     *
     * - `0` - Default priority
     * - `5` - Highest priority
     *
     * @see <a href="https://developer.android.com/reference/com/google/api/services/androidpublisher/model/TrackRelease#setInAppUpdatePriority(java.lang.Integer)">Google Play Developer API Reference</a>
     */
    @get:Input
    val updatePriority: Property<Int>
}
