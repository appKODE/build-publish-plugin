package ru.kode.android.build.publish.plugin.jira.config

import org.gradle.api.Named
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import ru.kode.android.build.publish.plugin.core.api.config.BasicAuthCredentials
import javax.inject.Inject

/**
 * Configuration for a Jira endpoint connection.
 *
 * This class defines the connection settings for a single Jira instance,
 * including the base URL, authentication credentials, and associated project keys.
 *
 * @see JiraEndpointsConfig For registering multiple endpoint configurations
 * @see BasicAuthCredentials For authentication configuration
 */
abstract class JiraEndpointConfig
    @Inject
    constructor(
        objects: ObjectFactory,
    ) : Named {
        /**
         * The base URL of the Jira instance.
         *
         * This should be the root URL of your Jira server, for example: `https://jira.example.com`
         */
        @get:Input
        abstract val baseUrl: Property<String>

        /**
         * Authentication credentials for connecting to the Jira instance.
         *
         * These credentials are used for Basic Authentication with the Jira REST API.
         * It is recommended to store sensitive values in `gradle.properties` or environment variables.
         *
         * @see BasicAuthCredentials
         */
        @get:Nested
        val credentials: BasicAuthCredentials =
            objects.newInstance(BasicAuthCredentials::class.java)

        /**
         * The set of Jira project keys associated with this endpoint.
         *
         * Project keys are used to identify which Jira projects this endpoint
         * configuration applies to. For example: `PROJ`, `MOBILE`, `ANDROID`.
         *
         * @see projectKey For adding a single project key
         * @see projectKeys For adding multiple project keys
         */
        @get:Input
        abstract val projectKeys: SetProperty<String>

        /**
         * Adds a single project key to this endpoint configuration.
         *
         * @param projectKey The Jira project key to add (e.g., "PROJ", "MOBILE")
         */
        fun projectKey(projectKey: String) {
            projectKeys.add(projectKey)
        }

        /**
         * Adds a single project key to this endpoint configuration using a Provider.
         *
         * @param projectKey A Provider that supplies the Jira project key to add (e.g., "PROJ", "MOBILE")
         */
        fun projectKey(projectKey: Provider<String>) {
            projectKeys.add(projectKey)
        }

        /**
         * Adds multiple project keys to this endpoint configuration.
         *
         * @param projectKey The Jira project keys to add (e.g., "PROJ", "MOBILE", "ANDROID")
         */
        fun projectKeys(vararg projectKey: String) {
            projectKeys.addAll(projectKey.toList())
        }

        /**
         * Adds multiple project keys to this endpoint configuration from an Iterable.
         *
         * @param projectKeys An Iterable of Jira project keys to add (e.g., listOf("PROJ", "MOBILE", "ANDROID"))
         */
        fun projectKeys(projectKeys: Iterable<String>) {
            this.projectKeys.addAll(projectKeys.toList())
        }

        /**
         * Adds multiple project keys to this endpoint configuration from a Provider.
         *
         * @param projectKeys A Provider that supplies a list of Jira project keys to add
         */
        fun projectKeys(projectKeys: Provider<List<String>>) {
            this.projectKeys.addAll(projectKeys)
        }
    }
