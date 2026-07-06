package ru.kode.android.build.publish.plugin.clickup.task.automation

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

/**
 * A fully-resolved, config-cache-safe description of one ClickUp project (workspace) targeted by a
 * [ClickUpAutomationTask].
 *
 * Instances are built by the task registrar from the plugin DSL (see `ClickUpTasksRegistrar`): each
 * project's optional automation patterns are already folded together with the automation-level
 * defaults. Carries **data only** — the [accountName] routes each action to the right account of the
 * task's single ClickUp service at execution time, and issues are routed to this binding by their
 * [taskIdPrefix].
 */
interface ClickUpProjectBinding {
    /**
     * The name of the `auth { account("…") }` this project lives on; used to select the account within
     * the task's single ClickUp service.
     */
    @get:Input
    val accountName: Property<String>

    /**
     * The ClickUp workspace (team) name this project maps to; used to resolve the custom fix-version
     * field and to scope custom-task-id lookups.
     */
    @get:Input
    val workspaceName: Property<String>

    /**
     * The custom-task-id prefix (e.g. `APP`). Issues whose key prefix matches this value are handled by
     * this binding.
     */
    @get:Input
    val taskIdPrefix: Property<String>

    @get:Input
    @get:Optional
    val fixVersionPattern: Property<String>

    @get:Input
    @get:Optional
    val fixVersionFieldName: Property<String>

    @get:Input
    @get:Optional
    val tagPattern: Property<String>
}
