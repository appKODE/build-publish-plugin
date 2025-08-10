package ru.kode.android.build.publish.plugin.appcenter.config

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile

/**
 * Configuration for authenticating with an Microsoft App Center project.
 */
interface AppCenterAuthConfig {
    val name: String

    /**
     * The path to the JSON file containing the API token for the App Center project.
     */
    @get:InputFile
    val apiTokenFile: RegularFileProperty

    /**
     * The owner name of the App Center project.
     */
    @get:Input
    val ownerName: Property<String>
}
