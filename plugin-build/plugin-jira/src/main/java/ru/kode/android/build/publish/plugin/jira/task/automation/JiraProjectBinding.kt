package ru.kode.android.build.publish.plugin.jira.task.automation

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

/**
 * A fully-resolved, config-cache-safe description of one Jira project targeted by a
 * [JiraAutomationTask].
 *
 * Instances are built by the task registrar from the plugin DSL (see `JiraTasksRegistrar`): each
 * project's optional automation patterns are already folded together with the automation-level
 * defaults, so the task only needs to read these values and route issues to the matching Jira
 * instance ([instanceName]).
 */
interface JiraProjectBinding {
    /**
     * The Jira project key (e.g. `PROJ`). Issues whose key prefix matches this value are handled by
     * this binding.
     */
    @get:Input
    val projectKey: Property<String>

    /**
     * The name of the `auth { }` configuration (Jira instance / credentials) to use for this project.
     * When absent, the task falls back to the variant-matched / common service.
     */
    @get:Input
    @get:Optional
    val instanceName: Property<String>

    @get:Input
    @get:Optional
    val labelPattern: Property<String>

    @get:Input
    @get:Optional
    val fixVersionPattern: Property<String>

    @get:Input
    @get:Optional
    val targetStatusName: Property<String>
}
