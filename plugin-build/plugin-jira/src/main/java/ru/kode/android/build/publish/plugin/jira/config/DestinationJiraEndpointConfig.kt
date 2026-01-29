package ru.kode.android.build.publish.plugin.jira.config

import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import javax.inject.Inject

/**
 * Configuration for mapping Jira project keys to a specific endpoint.
 *
 * This class defines which Jira endpoint configuration should be used for specific project keys.
 * It allows routing issues from different projects to their respective Jira instances.
 *
 * Example usage in build script:
 * ```
 * destinationEndpoint {
 *     endpointName = "myJiraEndpoint"
 *     projectKeys("PROJ", "MOBILE")
 * }
 * ```
 *
 * @see JiraAutomationConfig.destinationEndpoint For how to configure destination endpoints
 * @see JiraEndpointConfig For the endpoint configuration this maps to
 */
abstract class DestinationJiraEndpointConfig
    @Inject
    constructor() {
        @get:Input
        /**
         * The name of the Jira endpoint configuration to use for the specified project keys.
         *
         * This should match the name of an endpoint defined in the `endpoints` configuration block.
         *
         * @see JiraEndpointConfig For the endpoint configuration this references
         */
        abstract val endpointName: Property<String>

        /**
         * The set of Jira project keys that should be routed to the specified endpoint.
         *
         * Project keys are used to identify which Jira projects should use this endpoint
         * configuration. For example: `PROJ`, `MOBILE`, `ANDROID`.
         *
         * @see projectKey For adding a single project key
         * @see projectKeys For adding multiple project keys
         */
        @get:Input
        internal abstract val projectKeys: SetProperty<String>

        /**
         * Adds a single project key to this destination endpoint configuration.
         *
         * @param projectKey The Jira project key to add (e.g., "PROJ", "MOBILE")
         */
        fun projectKey(projectKey: String) {
            projectKeys.add(projectKey)
        }

        /**
         * Adds a single project key to this destination endpoint configuration from a Provider.
         *
         * @param projectKey A Provider that supplies the Jira project key to add
         */
        fun projectKey(projectKey: Provider<String>) {
            projectKeys.add(projectKey)
        }

        /**
         * Adds multiple project keys to this destination endpoint configuration.
         *
         * @param projectKey The Jira project keys to add (e.g., "PROJ", "MOBILE")
         */
        fun projectKeys(vararg projectKey: String) {
            projectKeys.addAll(projectKey.toList())
        }

        /**
         * Adds multiple project keys to this destination endpoint configuration from an Iterable.
         *
         * @param projectKeys An Iterable of Jira project keys to add
         */
        fun projectKeys(projectKeys: Iterable<String>) {
            this.projectKeys.addAll(projectKeys.toList())
        }

        /**
         * Adds multiple project keys to this destination endpoint configuration from a Provider.
         *
         * @param projectKeys A Provider that supplies a list of Jira project keys to add
         */
        fun projectKeys(projectKeys: Provider<List<String>>) {
            this.projectKeys.addAll(projectKeys)
        }
    }
