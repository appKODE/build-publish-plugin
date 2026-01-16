package ru.kode.android.build.publish.plugin.play.config

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile

/**
 * Configuration interface for Google Play authentication settings.
 *
 * This interface defines the authentication parameters required to publish an Android app
 * to the Google Play Store. It's used within the `BuildPublishPlayExtension` to
 * configure API access to the Google Play Developer API.
 */
interface PlayAuthConfig {
    /**
     * Name of this authentication configuration.
     *
     * Used to match a configuration to a build variant (or to the common configuration).
     */
    val name: String

    /**
     * The service account credentials file for Google Play API access.
     *
     * This should be a JSON file containing the service account credentials
     * with the necessary permissions to publish to the Google Play Store.
     *
     * To create a service account:
     * 1. Go to Google Play Console
     * 2. Navigate to Setup > API access
     * 3. Click "Create new service account"
     * 4. Follow the instructions to create and download the JSON key file
     *
     * Required permissions:
     * - `androidpublisher` scope
     * - Appropriate access level in Google Play Console
     *
     * @see <a href="https://developers.google.com/android-publisher/getting_started#setting_up_api_access">Setting up API access</a>
     */
    @get:InputFile
    val apiTokenFile: RegularFileProperty

    /**
     * The application ID (package name) in Google Play Console.
     *
     * This should match the `applicationId` in your app's build.gradle.kts file
     * and the package name in the Google Play Console.
     *
     * Example: `com.example.app`
     *
     * Note: This is different from the application name shown to users in the Play Store.
     */
    @get:Input
    val appId: Property<String>
}
