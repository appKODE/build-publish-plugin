package ru.kode.android.build.publish.plugin.nextcloud.config

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import ru.kode.android.build.publish.plugin.core.api.config.BasicAuthCredentials
import javax.inject.Inject

/**
 * Configuration for authenticating with a Nextcloud instance.
 *
 * This class holds the authentication settings required to connect to a Nextcloud server,
 * including the base URL and credentials. It's typically used as part of the Nextcloud
 * publishing configuration in your build script.
 */
abstract class NextcloudAuthConfig
    @Inject
    constructor(
        objects: ObjectFactory,
    ) {
        abstract val name: String

        /**
         * The base URL of the Nextcloud instance.
         *
         * This should be the root URL of your Nextcloud instance.
         *
         * Example: `https://cloud.example.com`
         */
        abstract val baseUrl: Property<String>

        /**
         * The credentials used to authenticate with the Nextcloud server.
         *
         * This nested configuration contains the username and password (or app password).
         *
         * @see BasicAuthCredentials
         */
        @get:Nested
        val credentials: BasicAuthCredentials =
            objects.newInstance(BasicAuthCredentials::class.java)
    }
