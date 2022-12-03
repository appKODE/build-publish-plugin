package ru.kode.android.build.publish.plugin.extension.config

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional

interface AppCenterDistributionConfig {
    val name: String

    /**
     * The path to JSON file with token for App Center project
     */
    @get:InputFile
    val apiTokenFile: RegularFileProperty

    /**
     * Owner name of the App Center project
     */
    @get:Input
    val ownerName: Property<String>

    /**
     * Short app name to be used as first part of app name in the AppCenter
     */
    @get:Input
    val appNamePrefix: Property<String>

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

internal const val MAX_REQUEST_COUNT = 20
internal const val MAX_REQUEST_DELAY_MS = 2000L
