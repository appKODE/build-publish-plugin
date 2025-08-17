package ru.kode.android.build.publish.plugin.jira.config

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

/**
 * Configuration for Jira automation rules that can be applied during the build process.
 *
 * This interface defines the configuration for automating Jira workflows, such as:
 * - Transitioning issues to different statuses
 * - Adding labels based on build information
 * - Updating fix versions
 */
interface JiraAutomationConfig {
    val name: String

    /**
     * The ID of the Jira project to apply automation to.
     *
     * This is typically a numeric ID that can be found in the Jira project settings
     * or by viewing the project in a browser and looking at the URL.
     */
    @get:Input
    val projectId: Property<Long>

    /**
     * Pattern for labels to be added to Jira issues.
     */
    @get:Input
    @get:Optional
    val labelPattern: Property<String>

    /**
     * Pattern for fix versions to be set on Jira issues.
     *
     * The version will be created in Jira if it doesn't already exist.
     * Supports the same variable substitution as [labelPattern].
     */
    @get:Input
    @get:Optional
    val fixVersionPattern: Property<String>

    /**
     * The ID of the status transition to apply to matching Jira issues.
     *
     * This is typically a string representing the transition ID in Jira's workflow.
     * Common values:
     * - `"5"` for "Resolved"
     * - `"2"` for "In Progress"
     *
     * To find the correct ID for a transition, you can:
     * 1. Use the Jira REST API browser
     * 2. Check your project's workflow configuration
     * 3. Contact your Jira administrator
     */
    @get:Input
    @get:Optional
    val resolvedStatusTransitionId: Property<String>
}
