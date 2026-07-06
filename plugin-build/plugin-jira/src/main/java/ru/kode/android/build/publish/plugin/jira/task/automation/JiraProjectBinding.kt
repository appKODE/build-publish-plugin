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
 * defaults. Carries **data only** — the [instanceName] routes each action to the right instance of the
 * task's single Jira service at execution time.
 */
interface JiraProjectBinding {
    /**
     * The Jira project key (e.g. `PROJ`). Issues whose key prefix matches this value are handled by
     * this binding.
     */
    @get:Input
    val projectKey: Property<String>

    /**
     * The name of the `auth { }` instance this project lives on; used to select the instance within the
     * task's single Jira service.
     */
    @get:Input
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
