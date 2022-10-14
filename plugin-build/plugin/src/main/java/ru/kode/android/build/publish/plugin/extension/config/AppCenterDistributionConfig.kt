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

    @get:Input
    @get:Optional
    val maxRequestCount: Property<Int>

    @get:Input
    @get:Optional
    val requestDelayMs: Property<Long>
}
