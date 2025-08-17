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
     * This determines how quickly the update is rolled out to users.
     * Higher values indicate higher priority.
     *
     * - `0` - Default priority (no special treatment)
     * - `1` - High priority (faster rollout)
     * - `2` - Critical priority (immediate rollout)
     *
     * Note: Higher priority updates may be subject to rate limiting by Google Play.
     *
     * @see <a href="https://developer.android.com/reference/com/google/api/services/androidpublisher/model/TrackRelease#setInAppUpdatePriority(java.lang.Integer)">Google Play Developer API Reference</a>
     */
    @get:Input
    val updatePriority: Property<Int>
}
