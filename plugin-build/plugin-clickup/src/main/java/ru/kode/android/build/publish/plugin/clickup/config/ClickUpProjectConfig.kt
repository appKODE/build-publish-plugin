package ru.kode.android.build.publish.plugin.clickup.config

import org.gradle.api.Named
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import ru.kode.android.build.publish.plugin.core.util.CommonConfigMergeable

/**
 * Per-project automation pattern override, declared inside a `targetAccount("…") { project("…") { … } }`
 * block.
 *
 * The project's workspace and task-id prefix come from the shared registry (see [ClickUpProjectDef]);
 * this override only tweaks the automation patterns for one referenced project. Any unset pattern falls
 * back to the automation-level default. Declaring `project("name") { … }` also selects that project.
 */
interface ClickUpProjectConfig : Named, CommonConfigMergeable<ClickUpProjectConfig> {
    /**
     * Pattern for the fix-version value set on ClickUp tasks of this project. Falls back to the
     * automation-level `fixVersionPattern`. Formatted with `buildVersion`, `buildNumber`, `buildVariant`.
     */
    @get:Input
    @get:Optional
    val fixVersionPattern: Property<String>

    /**
     * The custom-field name used for fix versions. Falls back to the automation-level
     * `fixVersionFieldName`.
     */
    @get:Input
    @get:Optional
    val fixVersionFieldName: Property<String>

    /**
     * Pattern for the tag added to ClickUp tasks of this project. Falls back to the automation-level
     * `tagPattern`.
     */
    @get:Input
    @get:Optional
    val tagPattern: Property<String>

    override fun inheritFrom(common: ClickUpProjectConfig) {
        fixVersionPattern.convention(common.fixVersionPattern)
        fixVersionFieldName.convention(common.fixVersionFieldName)
        tagPattern.convention(common.tagPattern)
    }
}
