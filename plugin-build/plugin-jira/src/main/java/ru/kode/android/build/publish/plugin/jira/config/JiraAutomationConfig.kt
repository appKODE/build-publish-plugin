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

    @get:Input

    /**
     * The Key of the Jira project to use for version management and fix version updates.
     *
     * This property is required if fix version updates are enabled.
     */
    val projectKey: Property<String>

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
     * The name of the target status to which issues will be transitioned.
     *
     * This is typically a string like "In Progress" or "Ready for Testing".
     * If not specified, no status transitions will be performed.
     */
    @get:Input
    @get:Optional
    val targetStatusName: Property<String>
}
