package ru.kode.android.build.publish.plugin.clickup.config

import org.gradle.api.Named
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import ru.kode.android.build.publish.plugin.core.util.CommonConfigMergeable

/**
 * A ClickUp project (workspace) declared in the shared registry, nested under an `auth` account:
 * `account("main") { project("app") { workspaceName.set("My Workspace"); taskIdPrefix.set("APP") } }`.
 *
 * The enclosing account is implicit from where the project is declared, so there is no `accountName`
 * here. Project names are unique per account; the [taskIdPrefix] must be globally unique so changelog
 * references can be routed by their custom-task-id prefix (e.g. `APP-123` -> `APP`).
 */
interface ClickUpProjectDef : Named, CommonConfigMergeable<ClickUpProjectDef> {
    /**
     * The ClickUp workspace (team) name this project maps to, used to resolve the `team_id` that scopes
     * custom-task-id lookups.
     */
    @get:Input
    val workspaceName: Property<String>

    /**
     * The ClickUp custom-task-id prefix (e.g. `"APP"`). References whose prefix matches this value
     * (`APP-123`) belong to this project. Must be globally unique across all accounts.
     */
    @get:Input
    val taskIdPrefix: Property<String>

    override fun inheritFrom(common: ClickUpProjectDef) {
        workspaceName.convention(common.workspaceName)
        taskIdPrefix.convention(common.taskIdPrefix)
    }
}
