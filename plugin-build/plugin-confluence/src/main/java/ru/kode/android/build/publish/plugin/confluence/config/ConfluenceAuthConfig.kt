package ru.kode.android.build.publish.plugin.confluence.config

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import ru.kode.android.build.publish.plugin.core.api.config.BasicAuthCredentials
import javax.inject.Inject

/**
 * Configuration for authenticating with a Confluence instance.
 *
 * This class holds the authentication settings required to connect to a Confluence server,
 * including the base URL and credentials. It's typically used as part of the Confluence
 * publishing configuration in your build script.
 */
abstract class ConfluenceAuthConfig
    @Inject
    constructor(
        objects: ObjectFactory,
    ) {
        abstract val name: String

        /**
         * The base URL of the Confluence instance.
         *
         * This should be the root URL of your Confluence instance, typically ending with "/wiki/".
         * For Confluence Cloud, it would be in the format: `https://your-domain.atlassian.net/wiki/`
         */
        abstract val baseUrl: Property<String>

        /**
         * The credentials used to authenticate with the Confluence server.
         *
         * This nested configuration contains the username and password (or API token)
         * required to authenticate with Confluence. For Confluence Cloud, it's recommended
         * to use an API token instead of your account password.
         *
         * @see BasicAuthCredentials
         */
        @get:Nested
        val credentials: BasicAuthCredentials =
            objects.newInstance(BasicAuthCredentials::class.java)
    }
