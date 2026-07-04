package ru.kode.android.build.publish.plugin.jira.config

import org.gradle.api.Named
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import ru.kode.android.build.publish.plugin.core.util.CommonConfigMergeable

/**
 * A Jira project declared in the shared registry, nested under an `auth` instance:
 * `instance("default") { projects { project("app") { projectKey.set("APP") } } }`.
 *
 * The enclosing instance is implicit from where the project is declared, so there is no `instanceName`
 * here. Project names are unique per instance (like Telegram chat names per bot); the [projectKey]
 * must be globally unique so changelog issues can be routed by their key prefix (e.g. `APP-123` ->
 * `APP`).
 */
interface JiraProjectDef : Named, CommonConfigMergeable<JiraProjectDef> {
    /**
     * The Jira project key (e.g. `"APP"`). Issues whose key prefix matches this value belong to this
     * project.
     */
    @get:Input
    val projectKey: Property<String>

    override fun inheritFrom(common: JiraProjectDef) {
        projectKey.convention(common.projectKey)
    }
}
