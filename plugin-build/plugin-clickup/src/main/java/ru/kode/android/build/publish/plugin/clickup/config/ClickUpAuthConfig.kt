package ru.kode.android.build.publish.plugin.clickup.config

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile

/**
 * Configuration for authenticating with the ClickUp API.
 *
 * This interface defines the authentication settings required to interact with the ClickUp API.
 * It's typically configured in the build script using the `auth` block of the `buildPublishClickUp` extension.
 */
interface ClickUpAuthConfig {
    val name: String

    /**
     * The file containing the ClickUp API token used for authentication.
     *
     * The file should contain a single line with the API token. For security reasons,
     * it's recommended to store this file outside of version control and use environment
     * variables or a secure secret management solution in CI/CD environments.
     *
     * To generate an API token:
     * 1. Log in to your ClickUp account
     * 2. Click on your profile picture in the bottom left corner
     * 3. Go to "Apps" > "Generate" under the Developer API section
     * 4. Copy the generated token to a secure file
     */
    @get:InputFile
    val apiTokenFile: RegularFileProperty
}
