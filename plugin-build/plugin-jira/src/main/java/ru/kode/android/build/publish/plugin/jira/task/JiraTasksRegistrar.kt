package ru.kode.android.build.publish.plugin.jira.task

import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.util.capitalizedName
import ru.kode.android.build.publish.plugin.jira.core.JiraConfig
import ru.kode.android.build.publish.plugin.jira.task.automation.JiraAutomationTask

internal const val JIRA_AUTOMATION_TASK = "jiraAutomation"

object JiraTasksRegistrar {

    fun registerAutomationTask(
        project: TaskContainer,
        config: JiraConfig,
        params: JiraAutomationTaskParams,
    ): TaskProvider<JiraAutomationTask>? {
        return project.registerJiraTasks(config, params)
    }
}

private fun TaskContainer.registerJiraTasks(
    config: JiraConfig,
    params: JiraAutomationTaskParams,
): TaskProvider<JiraAutomationTask>? {
    return if (
        config.labelPattern.isPresent ||
        config.fixVersionPattern.isPresent ||
        config.resolvedStatusTransitionId.isPresent
    ) {
        register(
            "$JIRA_AUTOMATION_TASK${params.buildVariant.capitalizedName()}",
            JiraAutomationTask::class.java,
        ) {
            it.tagBuildFile.set(params.tagBuildProvider)
            it.changelogFile.set(params.changelogFileProvider)
            it.issueNumberPattern.set(params.issueNumberPattern)
            it.baseUrl.set(config.baseUrl)
            it.username.set(config.authUsername)
            it.projectId.set(config.projectId)
            it.password.set(config.authPassword)
            it.labelPattern.set(config.labelPattern)
            it.fixVersionPattern.set(config.fixVersionPattern)
            it.resolvedStatusTransitionId.set(config.resolvedStatusTransitionId)
        }
    } else {
        null
    }
}

data class JiraAutomationTaskParams(
    val buildVariant: BuildVariant,
    val issueNumberPattern: Provider<String>,
    val changelogFileProvider: Provider<RegularFile>,
    val tagBuildProvider: Provider<RegularFile>,
)
