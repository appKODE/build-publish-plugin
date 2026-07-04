package ru.kode.android.build.publish.plugin.jira.config

import org.gradle.api.Named
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import ru.kode.android.build.publish.plugin.core.util.CommonConfigMergeable

/**
 * Per-project automation pattern override, declared inside a `targetInstance("…") { project("…") { … } }`
 * block.
 *
 * The project's key and Jira instance come from the shared registry (see [JiraProjectDef]); this
 * override only tweaks the automation patterns for one referenced project. Any unset pattern falls back
 * to the automation-level default. Declaring `project("name") { … }` also selects that project.
 */
interface JiraProjectConfig : Named, CommonConfigMergeable<JiraProjectConfig> {
    /**
     * Pattern for labels added to Jira issues of this project. Falls back to the automation-level
     * `labelPattern`. Formatted with `buildVersion`, `buildNumber`, `buildVariant`.
     */
    @get:Input
    @get:Optional
    val labelPattern: Property<String>

    /**
     * Pattern for fix versions set on Jira issues of this project. Falls back to the automation-level
     * `fixVersionPattern`.
     */
    @get:Input
    @get:Optional
    val fixVersionPattern: Property<String>

    /**
     * Target status to which issues of this project are transitioned. Falls back to the
     * automation-level `targetStatusName`.
     */
    @get:Input
    @get:Optional
    val targetStatusName: Property<String>

    override fun inheritFrom(common: JiraProjectConfig) {
        labelPattern.convention(common.labelPattern)
        fixVersionPattern.convention(common.fixVersionPattern)
        targetStatusName.convention(common.targetStatusName)
    }
}
