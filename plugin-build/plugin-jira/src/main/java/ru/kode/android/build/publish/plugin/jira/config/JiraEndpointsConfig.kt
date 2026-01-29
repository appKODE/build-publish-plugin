package ru.kode.android.build.publish.plugin.jira.config

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

/**
 * Configuration container for Jira endpoint settings.
 *
 * This class holds a collection of [JiraEndpointConfig] instances, each defining
 * connection details for a specific Jira server endpoint including base URL,
 * credentials, and associated project keys.
 *
 * @see JiraEndpointConfig For individual endpoint configuration options
 * @see BuildPublishJiraExtension For how this is used in the plugin extension
 */
abstract class JiraEndpointsConfig
    @Inject
    constructor(
        objects: ObjectFactory,
    ) : Named {
        /**
         * Internal container for storing endpoint configurations.
         *
         * This property holds all endpoint configurations added via the [endpoint] method.
         * Access to this container is restricted to the plugin implementation.
         */
        internal val endpoints: NamedDomainObjectContainer<JiraEndpointConfig> =
            objects.domainObjectContainer(JiraEndpointConfig::class.java)

        /**
         * Registers a new Jira endpoint configuration with the specified name.
         *
         * This method creates and configures a new [JiraEndpointConfig] instance
         * that defines connection details for a Jira server, including base URL,
         * authentication credentials, and associated project keys.
         *
         * @param endpointName The unique name to identify this endpoint configuration
         * @param action Configuration action to apply to the endpoint
         *
         * @see JiraEndpointConfig For available configuration options
         */
        fun endpoint(
            endpointName: String,
            action: Action<JiraEndpointConfig>,
        ) {
            endpoints.register(endpointName, action)
        }
    }
