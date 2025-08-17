package ru.kode.android.build.publish.plugin.appcenter.config

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

internal const val MAX_REQUEST_COUNT = 20
internal const val MAX_REQUEST_DELAY_MS = 2000L

/**
 * Configuration for distributing builds to AppCenter.
 *
 * This class holds all the necessary configuration for uploading and distributing
 * Android builds to AppCenter, including authentication, distribution groups,
 * and upload behavior settings.
 *
 * The constants [MAX_REQUEST_COUNT] and [MAX_REQUEST_DELAY_MS] define the plugin's default behavior
 * when polling AppCenter for upload completion.
 */
abstract class AppCenterDistributionConfig {
    abstract val name: String

    /**
     * Application name in AppCenter.
     *
     * This should match the exact name of your app as it appears in the AppCenter portal.
     *
     * If this value is not explicitly set, the plugin will automatically generate a name
     * using the format: `<baseFileName>-<variantName>`, for example:
     * ```
     * example-base-project-android-debug
     * example-base-project-android-internal
     * ```
     *
     * **Note**: The app must exist in AppCenter before you can upload builds to it.
     */
    @get:Input
    abstract val appName: Property<String>

    /**
     * Set of test groups in AppCenter to which the build will be distributed.
     *
     * You can add groups using the convenience methods [testerGroup] or [testerGroups].
     *
     * ## Example
     * ```kotlin
     * testerGroup("qa-team")
     * testerGroups("beta-testers", "internal")
     * ```
     *
     * **Note**: The groups must exist in your AppCenter organization before you can distribute to them.
     */
    @get:Input
    internal abstract val testerGroups: SetProperty<String>

    /**
     * Maximum number of requests to send when polling upload status.
     *
     * This controls how many times the plugin will check the status of an upload
     * before giving up. Each request is subject to the delay specified by
     * [uploadStatusRequestDelayMs] or calculated by [uploadStatusRequestDelayCoefficient].
     *
     * If not specified, defaults to [MAX_REQUEST_COUNT] (20).
     *
     * ## Example
     * ```kotlin
     * // Check up to 30 times before giving up
     * maxUploadStatusRequestCount.set(30)
     * ```
     */
    @get:Input
    @get:Optional
    abstract val maxUploadStatusRequestCount: Property<Int>

    /**
     * Delay in milliseconds between upload status polling requests.
     *
     * This sets a fixed delay between status checks. For dynamic delay based on APK size,
     * use [uploadStatusRequestDelayCoefficient] instead.
     *
     * If not specified, defaults to [MAX_REQUEST_DELAY_MS] (2000ms).
     *
     * **Note**: If [uploadStatusRequestDelayCoefficient] is set, this value will be ignored.
     *
     * ## Example
     * ```kotlin
     * // Check every 3 seconds
     * uploadStatusRequestDelayMs.set(3000L)
     * ```
     */
    @get:Input
    @get:Optional
    abstract val uploadStatusRequestDelayMs: Property<Long>

    /**
     * Optional coefficient `K` used to calculate dynamic upload status request delay:
     *
     * ```
     * delaySecs = apkSizeMb / K
     * ```
     *
     * This allows the delay between status checks to scale with the size of the APK.
     * For example, with K=5 and a 50MB APK, the delay would be 10 seconds.
     *
     * If this is not specified or is set to `0`, [uploadStatusRequestDelayMs] will be used instead.
     *
     * ## Example
     * ```kotlin
     * // For a 50MB APK, this would result in a 10-second delay between checks
     * uploadStatusRequestDelayCoefficient.set(5)
     * ```
     */
    @get:Input
    @get:Optional
    abstract val uploadStatusRequestDelayCoefficient: Property<Long>

    /**
     * Adds a single tester group to receive this build.
     *
     * @param testerGroup The name of the tester group to add
     * @see testerGroups
     */
    fun testerGroup(testerGroup: String) {
        testerGroups.add(testerGroup)
    }

    /**
     * Adds multiple tester groups to receive this build.
     *
     * @param testerGroup Vararg of tester group names to add
     * @see testerGroup
     */
    fun testerGroups(vararg testerGroup: String) {
        testerGroups.addAll(testerGroup.toList())
    }
}
