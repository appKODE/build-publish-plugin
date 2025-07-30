package ru.kode.android.build.publish.plugin.clickup.core

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import ru.kode.android.build.publish.plugin.clickup.task.ClickUpAutomationTask
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.util.capitalizedName

internal const val CLICK_UP_AUTOMATION_TASK = "clickUpAutomation"

interface ClickUpConfig {
    val name: String

    /**
     * The path to the file containing the API token for the ClickUp
     */
    @get:InputFile
    val apiTokenFile: RegularFileProperty

    /**
     * Pattern to be used to format version to the ClickUp tasks
     */
    @get:Input
    @get:Optional
    val fixVersionPattern: Property<String>

    /**
     * The id of the custom field to be used for the fix version in the ClickUp tasks
     */
    @get:Input
    @get:Optional
    val fixVersionFieldId: Property<String>

    /**
     * The tag name to be used for the ClickUp tasks
     */
    @get:Input
    @get:Optional
    val tagName: Property<String>

    fun registerAutomationTask(
        project: Project,
        params: ClickUpAutomationTaskParams,
    ): TaskProvider<ClickUpAutomationTask>? {
        return project.tasks.registerClickUpTasks(this, params)
    }
}

private fun TaskContainer.registerClickUpTasks(
    config: ClickUpConfig,
    params: ClickUpAutomationTaskParams,
): TaskProvider<ClickUpAutomationTask>? {
    val fixVersionIsPresent =
        config.fixVersionPattern.isPresent && config.fixVersionFieldId.isPresent
    val hasMissingFixVersionProperties =
        config.fixVersionPattern.isPresent || config.fixVersionFieldId.isPresent

    if (!fixVersionIsPresent && hasMissingFixVersionProperties) {
        throw GradleException(
            "To use the fixVersion logic, the fixVersionPattern or fixVersionFieldId " +
                "properties must be specified",
        )
    }

    return if (fixVersionIsPresent || config.tagName.isPresent) {
        register(
            "$CLICK_UP_AUTOMATION_TASK${params.buildVariant.capitalizedName()}",
            ClickUpAutomationTask::class.java,
        ) {
            it.tagBuildFile.set(params.tagBuildProvider)
            it.changelogFile.set(params.changelogFileProvider)
            it.issueNumberPattern.set(params.issueNumberPattern)
            it.apiTokenFile.set(config.apiTokenFile)
            it.fixVersionPattern.set(config.fixVersionPattern)
            it.fixVersionFieldId.set(config.fixVersionFieldId)
            it.taskTag.set(config.tagName)
        }
    } else {
        null
    }
}

data class ClickUpAutomationTaskParams(
    val buildVariant: BuildVariant,
    val issueNumberPattern: Provider<String>,
    val changelogFileProvider: Provider<RegularFile>,
    val tagBuildProvider: Provider<RegularFile>,
)
