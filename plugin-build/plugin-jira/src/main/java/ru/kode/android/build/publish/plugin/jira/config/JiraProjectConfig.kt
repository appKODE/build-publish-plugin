package ru.kode.android.build.publish.plugin.jira.config

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import ru.kode.android.build.publish.plugin.core.util.CommonConfigMergeable

/**
 * Configuration for a single Jira project targeted by an automation rule.
 *
 * A [JiraAutomationConfig] may declare multiple projects (via its `projects { }` container), each
 * pointing at its own Jira project key and, optionally, its own Jira instance (via [instanceName]).
 * Changelog issues are routed to a project by their issue-key prefix (e.g. `PROJ-123` -> `PROJ`).
 *
 * Every optional automation rule ([labelPattern], [fixVersionPattern], [targetStatusName]) falls
 * back to the value declared on the parent [JiraAutomationConfig] when it is not set here.
 */
interface JiraProjectConfig : CommonConfigMergeable<JiraProjectConfig> {
    val name: String

    /**
     * The Key of the Jira project to use for version management and fix version updates.
     *
     * Issues whose key starts with this prefix (case-insensitive) are routed to this project.
     */
    @get:Input
    val projectKey: Property<String>

    /**
     * The name of the `auth { }` configuration (and therefore the Jira instance / credentials) to
     * use for this project.
     *
     * When omitted, the automation task falls back to the auth configuration matching the build
     * variant name, then to the common (`default`) auth configuration — preserving single-instance
     * behavior.
     */
    @get:Input
    @get:Optional
    val instanceName: Property<String>

    /**
     * Pattern for labels to be added to Jira issues of this project.
     *
     * Falls back to the parent automation `labelPattern` when not set.
     * Formatted using `String.format(...)` with `buildVersion`, `buildNumber`, `buildVariant`.
     */
    @get:Input
    @get:Optional
    val labelPattern: Property<String>

    /**
     * Pattern for fix versions to be set on Jira issues of this project.
     *
     * Falls back to the parent automation `fixVersionPattern` when not set.
     * Formatted using `String.format(...)` with `buildVersion`, `buildNumber`, `buildVariant`.
     */
    @get:Input
    @get:Optional
    val fixVersionPattern: Property<String>

    /**
     * The name of the target status to which issues of this project will be transitioned.
     *
     * Falls back to the parent automation `targetStatusName` when not set.
     */
    @get:Input
    @get:Optional
    val targetStatusName: Property<String>

    override fun inheritFrom(common: JiraProjectConfig) {
        projectKey.convention(common.projectKey)
        instanceName.convention(common.instanceName)
        labelPattern.convention(common.labelPattern)
        fixVersionPattern.convention(common.fixVersionPattern)
        targetStatusName.convention(common.targetStatusName)
    }
}
