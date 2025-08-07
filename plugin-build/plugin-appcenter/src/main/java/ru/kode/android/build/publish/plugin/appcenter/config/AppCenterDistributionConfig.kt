package ru.kode.android.build.publish.plugin.appcenter.config

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

internal const val MAX_REQUEST_COUNT = 20
internal const val MAX_REQUEST_DELAY_MS = 2000L

interface AppCenterDistributionConfig {
    val name: String

    /**
     * "Application name in AppCenter. If appName isn't set plugin uses <baseFileName>-<variantName>,
     * for example example-base-project-android-debug, example-base-project-android-internal"
     */
    @get:Input
    val appName: Property<String>

    /**
     * Test groups for app distribution
     *
     * For example: [android-testers]
     */
    @get:Input
    val testerGroups: SetProperty<String>

    /**
     * Max request count to check upload status. Default = [MAX_REQUEST_COUNT]
     */
    @get:Input
    @get:Optional
    val maxUploadStatusRequestCount: Property<Int>

    /**
     * Request delay in ms for each request. Default = [MAX_REQUEST_DELAY_MS] ms
     */
    @get:Input
    @get:Optional
    val uploadStatusRequestDelayMs: Property<Long>

    /**
     * Coefficient K for dynamic upload status request delay calculation:
     *
     * delaySecs = apkSizeMb / K
     *
     * If this isn't specified or 0, [uploadStatusRequestDelayMs] will be used.
     *
     * Default value is null.
     */
    @get:Input
    @get:Optional
    val uploadStatusRequestDelayCoefficient: Property<Long>
}
