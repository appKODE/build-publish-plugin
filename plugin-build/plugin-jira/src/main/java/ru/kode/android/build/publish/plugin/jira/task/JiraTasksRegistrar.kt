package ru.kode.android.build.publish.plugin.jira.task

import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.util.capitalizedName
import ru.kode.android.build.publish.plugin.jira.core.JiraAuthConfig
import ru.kode.android.build.publish.plugin.jira.core.JiraAutomationConfig
import ru.kode.android.build.publish.plugin.jira.task.automation.JiraAutomationTask

internal const val JIRA_AUTOMATION_TASK = "jiraAutomation"

object JiraTasksRegistrar {

    fun registerAutomationTask(
        project: TaskContainer,
        authConfig: JiraAuthConfig,
        automationConfig: JiraAutomationConfig,
        params: JiraAutomationTaskParams,
    ): TaskProvider<JiraAutomationTask>? {
        return project.registerJiraTasks(authConfig, automationConfig, params)
    }
}

private fun TaskContainer.registerJiraTasks(
    authConfig: JiraAuthConfig,
    automationConfig: JiraAutomationConfig,
    params: JiraAutomationTaskParams,
): TaskProvider<JiraAutomationTask>? {
    return if (
        automationConfig.labelPattern.isPresent ||
        automationConfig.fixVersionPattern.isPresent ||
        automationConfig.resolvedStatusTransitionId.isPresent
    ) {
        register(
            "$JIRA_AUTOMATION_TASK${params.buildVariant.capitalizedName()}",
            JiraAutomationTask::class.java,
        ) {
            it.tagBuildFile.set(params.tagBuildProvider)
            it.changelogFile.set(params.changelogFileProvider)
            it.issueNumberPattern.set(params.issueNumberPattern)
            it.baseUrl.set(authConfig.baseUrl)
            it.username.set(authConfig.authUsername)
            it.projectId.set(automationConfig.projectId)
            it.password.set(authConfig.authPassword)
            it.labelPattern.set(automationConfig.labelPattern)
            it.fixVersionPattern.set(automationConfig.fixVersionPattern)
            it.resolvedStatusTransitionId.set(automationConfig.resolvedStatusTransitionId)
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
