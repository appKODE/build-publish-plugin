package ru.kode.android.build.publish.plugin.jira.config

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import ru.kode.android.build.publish.plugin.core.api.config.BasicAuthCredentials
import javax.inject.Inject

/**
 * Configuration for authenticating with a Jira instance.
 *
 * This class holds the necessary credentials and connection details
 * required to authenticate with a Jira server or cloud instance.
 *
 * @see BasicAuthCredentials For credential configuration options
 */
abstract class JiraAuthConfig
    @Inject
    constructor(
        objects: ObjectFactory,
    ) {
        abstract val name: String

        /**
         * The base URL of the Jira instance.
         *
         * This should be the root URL of your Jira instance, for example:
         * - Cloud: `https://your-domain.atlassian.net`
         * - Server: `https://jira.your-company.com`
         */
        @get:Input
        abstract val baseUrl: Property<String>

        /**
         * The credentials used to authenticate with the Jira instance.
         *
         * For Jira Cloud, use an API token as the password.
         * For Jira Server, you can use either a username/password or an API token.
         *
         * @see BasicAuthCredentials For available credential configuration options
         */
        @get:Nested
        val credentials: BasicAuthCredentials =
            objects.newInstance(BasicAuthCredentials::class.java)
    }
