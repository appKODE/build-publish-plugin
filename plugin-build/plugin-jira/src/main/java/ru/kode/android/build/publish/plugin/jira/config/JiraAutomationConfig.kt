package ru.kode.android.build.publish.plugin.jira.config

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import javax.inject.Inject

/**
 * Configuration for Jira automation rules that can be applied during the build process.
 *
 * This interface defines the configuration for automating Jira workflows, such as:
 * - Transitioning issues to different statuses
 * - Adding labels based on build information
 * - Updating fix versions
 */
abstract class JiraAutomationConfig
    @Inject
    constructor(
        private val objects: ObjectFactory,
    ) {
        abstract val name: String

        /**
         * Set of destination endpoint configurations for routing issues to specific Jira instances.
         *
         * Each destination endpoint maps a set of project keys to a named Jira endpoint configuration.
         * This allows routing issues from different projects to their respective Jira instances.
         *
         * @see destinationEndpoint For configuring individual destination endpoints
         * @see DestinationJiraEndpointConfig For the configuration options available
         */
        @get:Input
        @get:Optional
        internal abstract val destinationEndpoints: SetProperty<DestinationJiraEndpointConfig>

        /**
         * Pattern for labels to be added to Jira issues.
         *
         * This pattern is formatted using `String.format(...)` and receives:
         * - `buildVersion`
         * - `buildNumber`
         * - `buildVariant`
         */
        @get:Input
        @get:Optional
        abstract val labelPattern: Property<String>

        /**
         * Pattern for fix versions to be set on Jira issues.
         *
         * The version will be created in Jira if it doesn't already exist.
         *
         * This pattern is formatted using `String.format(...)` and receives:
         * - `buildVersion`
         * - `buildNumber`
         * - `buildVariant`
         */
        @get:Input
        @get:Optional
        abstract val fixVersionPattern: Property<String>

        /**
         * The name of the target status to which issues will be transitioned.
         *
         * This is typically a string like "In Progress" or "Ready for Testing".
         * If not specified, no status transitions will be performed.
         */
        @get:Input
        @get:Optional
        abstract val targetStatusName: Property<String>

        /**
         * Configures a destination endpoint mapping for Jira automation.
         *
         * This defines which Jira endpoint configuration should be used for specific project keys.
         * Multiple destination endpoints can be configured to route issues from different projects
         * to their respective Jira instances.
         *
         * @param action The configuration action to apply to the destination endpoint
         * @see DestinationJiraEndpointConfig For available configuration options
         */
        fun destinationEndpoint(action: Action<DestinationJiraEndpointConfig>) {
            val destinationEndpoint = objects.newInstance(DestinationJiraEndpointConfig::class.java)
            action.execute(destinationEndpoint)
            destinationEndpoints.add(destinationEndpoint)
        }
    }
