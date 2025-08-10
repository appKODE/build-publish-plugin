package ru.kode.android.build.publish.plugin.appcenter.config

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

internal const val MAX_REQUEST_COUNT = 20
internal const val MAX_REQUEST_DELAY_MS = 2000L

/**
 * Configuration for distributing builds to Microsoft App Center.
 *
 * The constants [MAX_REQUEST_COUNT] and [MAX_REQUEST_DELAY_MS] define the plugin's default behavior
 * when polling App Center for upload completion.
 */
interface AppCenterDistributionConfig {
    val name: String

    /**
     * Application name in App Center.
     *
     * If this value is not explicitly set, the plugin uses the format:
     * `<baseFileName>-<variantName>`, for example:
     * ```
     * example-base-project-android-debug
     * example-base-project-android-internal
     * ```
     */
    @get:Input
    val appName: Property<String>

    /**
     * Set of test groups in App Center to which the build will be distributed.
     *
     * Example:
     * ```
     * ["android-testers"]
     * ```
     */
    @get:Input
    val testerGroups: SetProperty<String>

    /**
     * Maximum number of requests to send when polling upload status.
     *
     * If not specified, defaults to [MAX_REQUEST_COUNT].
     */
    @get:Input
    @get:Optional
    val maxUploadStatusRequestCount: Property<Int>

    /**
     * Delay in milliseconds between upload status polling requests.
     *
     * If not specified, defaults to [MAX_REQUEST_DELAY_MS].
     */
    @get:Input
    @get:Optional
    val uploadStatusRequestDelayMs: Property<Long>

    /**
     * Optional coefficient `K` used to calculate dynamic upload status request delay:
     *
     * ```
     * delaySecs = apkSizeMb / K
     * ```
     *
     * If this is not specified or is set to `0`, [uploadStatusRequestDelayMs] will be used instead.
     */
    @get:Input
    @get:Optional
    val uploadStatusRequestDelayCoefficient: Property<Long>
}
