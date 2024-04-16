package ru.kode.android.build.publish.plugin.extension.config

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile

interface PlayConfig {
    val name: String

    /**
     * The path to file with token for Google Play project
     */
    @get:InputFile
    val apiTokenFile: RegularFileProperty

    /**
     * appId in Google Play
     */
    @get:Input
    val appId: Property<String>

    /**
     * Track name of target app. Defaults to "internal"
     */
    @get:Input
    val trackId: Property<String>

    /**
     * Test groups for app distribution
     *
     * For example: [android-testers]
     */
    @get:Input
    val updatePriority: Property<Int>
}
