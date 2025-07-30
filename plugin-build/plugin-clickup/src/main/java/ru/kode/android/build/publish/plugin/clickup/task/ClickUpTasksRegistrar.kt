package ru.kode.android.build.publish.plugin.clickup.task

import org.gradle.api.GradleException
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import ru.kode.android.build.publish.plugin.clickup.core.ClickUpConfig
import ru.kode.android.build.publish.plugin.clickup.task.automation.ClickUpAutomationTask
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.util.capitalizedName

internal const val CLICK_UP_AUTOMATION_TASK = "clickUpAutomation"

object ClickUpTasksRegistrar {

    fun registerAutomationTask(
        project: TaskContainer,
        config: ClickUpConfig,
        params: ClickUpAutomationTaskParams,
    ): TaskProvider<ClickUpAutomationTask>? {
        return project.registerClickUpTasks(config, params)
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
