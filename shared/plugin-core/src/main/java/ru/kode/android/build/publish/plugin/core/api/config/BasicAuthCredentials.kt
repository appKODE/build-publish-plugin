package ru.kode.android.build.publish.plugin.core.api.config

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

/**
 * A data class representing Basic Authentication credentials.
 *
 * This class encapsulates the username and password required for HTTP Basic Authentication.
 * It's typically used for authenticating with remote services that require basic auth.
 *
 * ## Security Note
 * Always use secure methods to provide credentials, such as environment variables or
 * Gradle properties, and never commit actual credentials to version control.
 */
abstract class BasicAuthCredentials {
    /**
     * The username for Basic Authentication.
     *
     * This is typically an API key ID or service account username.
     **/
    @get:Input
    abstract val username: Property<String>

    /**
     * The password or API key secret for Basic Authentication.
     *
     * This is typically an API key secret or service account password.
     *
     * Security: This is a sensitive value. Use environment variables or secure properties.
     */
    @get:Input
    abstract val password: Property<String>
}
