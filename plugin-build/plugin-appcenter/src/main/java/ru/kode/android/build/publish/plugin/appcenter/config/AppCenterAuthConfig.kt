package ru.kode.android.build.publish.plugin.appcenter.config

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile

/**
 * Configuration for authenticating with AppCenter project.
 *
 * This interface defines the authentication settings required to interact with the AppCenter API.
 * It's used to configure API token-based authentication for build distribution.
 */
interface AppCenterAuthConfig {
    val name: String

    /**
     * The file containing the API token for authenticating with AppCenter.
     *
     * The file should contain a valid AppCenter API token with appropriate permissions.
     */
    @get:InputFile
    val apiTokenFile: RegularFileProperty

    /**
     * The owner name of the AppCenter project.
     *
     * This is typically your organization name or username in AppCenter.
     */
    @get:Input
    val ownerName: Property<String>
}
