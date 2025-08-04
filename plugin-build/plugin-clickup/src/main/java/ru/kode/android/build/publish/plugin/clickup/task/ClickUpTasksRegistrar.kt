package ru.kode.android.build.publish.plugin.clickup.task

import org.gradle.api.GradleException
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import ru.kode.android.build.publish.plugin.clickup.core.ClickUpAuthConfig
import ru.kode.android.build.publish.plugin.clickup.core.ClickUpAutomationConfig
import ru.kode.android.build.publish.plugin.clickup.task.automation.ClickUpAutomationTask
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.util.capitalizedName

internal const val CLICK_UP_AUTOMATION_TASK = "clickUpAutomation"

object ClickUpTasksRegistrar {

    fun registerAutomationTask(
        project: TaskContainer,
        authConfig: ClickUpAuthConfig,
        automationConfig: ClickUpAutomationConfig,
        params: ClickUpAutomationTaskParams,
    ): TaskProvider<ClickUpAutomationTask>? {
        return project.registerClickUpTasks(authConfig, automationConfig, params)
    }
}

private fun TaskContainer.registerClickUpTasks(
    config: ClickUpAuthConfig,
    automationConfig: ClickUpAutomationConfig,
    params: ClickUpAutomationTaskParams,
): TaskProvider<ClickUpAutomationTask>? {
    val fixVersionIsPresent =
        automationConfig.fixVersionPattern.isPresent && automationConfig.fixVersionFieldId.isPresent
    val hasMissingFixVersionProperties =
        automationConfig.fixVersionPattern.isPresent || automationConfig.fixVersionFieldId.isPresent

    if (!fixVersionIsPresent && hasMissingFixVersionProperties) {
        throw GradleException(
            "To use the fixVersion logic, the fixVersionPattern or fixVersionFieldId " +
                "properties must be specified",
        )
    }

    return if (fixVersionIsPresent || automationConfig.tagName.isPresent) {
        register(
            "$CLICK_UP_AUTOMATION_TASK${params.buildVariant.capitalizedName()}",
            ClickUpAutomationTask::class.java,
        ) {
            it.tagBuildFile.set(params.tagBuildProvider)
            it.changelogFile.set(params.changelogFileProvider)
            it.issueNumberPattern.set(params.issueNumberPattern)
            it.apiTokenFile.set(config.apiTokenFile)
            it.fixVersionPattern.set(automationConfig.fixVersionPattern)
            it.fixVersionFieldId.set(automationConfig.fixVersionFieldId)
            it.taskTag.set(automationConfig.tagName)
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
