package ru.kode.android.build.publish.plugin.extension.config

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input

interface AppCenterDistributionConfig {
    val name: String

    /**
     * The path to JSON file with token for App Center project
     */
    @get:Input
    val apiTokenFilePath: Property<String>

    /**
     * Owner name of the App Center project
     */
    @get:Input
    val ownerName: Property<String>

    /**
     * Short app name to be used as first part of app name in the AppCenter
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
}
