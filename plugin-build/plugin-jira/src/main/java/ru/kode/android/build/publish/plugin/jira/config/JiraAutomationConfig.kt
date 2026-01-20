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

    @get:Input
    @get:Optional
    internal abstract val destinationAuths: SetProperty<DestinationJiraAuthConfig>

    fun destinationAuth(action: Action<DestinationJiraAuthConfig>) {
        val destinationAuth = objects.newInstance(DestinationJiraAuthConfig::class.java)
        action.execute(destinationAuth)
        destinationAuths.add(destinationAuth)
    }

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
}
