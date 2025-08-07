package ru.kode.android.build.publish.plugin.jira.task

import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.util.capitalizedName
import ru.kode.android.build.publish.plugin.core.util.flatMapByNameOrDefault
import ru.kode.android.build.publish.plugin.jira.core.JiraAutomationConfig
import ru.kode.android.build.publish.plugin.jira.service.JiraNetworkServiceExtension
import ru.kode.android.build.publish.plugin.jira.task.automation.JiraAutomationTask

internal const val JIRA_AUTOMATION_TASK = "jiraAutomation"

object JiraTasksRegistrar {
    fun registerAutomationTask(
        project: Project,
        automationConfig: JiraAutomationConfig,
        params: JiraAutomationTaskParams,
    ): TaskProvider<JiraAutomationTask>? {
        return project.registerJiraTasks(automationConfig, params)
    }
}

private fun Project.registerJiraTasks(
    automationConfig: JiraAutomationConfig,
    params: JiraAutomationTaskParams,
): TaskProvider<JiraAutomationTask>? {
    return if (
        automationConfig.labelPattern.isPresent ||
        automationConfig.fixVersionPattern.isPresent ||
        automationConfig.resolvedStatusTransitionId.isPresent
    ) {
        val networkService =
            project.extensions
                .getByType(JiraNetworkServiceExtension::class.java)
                .services
                .flatMapByNameOrDefault(params.buildVariant.name)

        tasks.register(
            "$JIRA_AUTOMATION_TASK${params.buildVariant.capitalizedName()}",
            JiraAutomationTask::class.java,
        ) {
            it.tagBuildFile.set(params.tagBuildProvider)
            it.changelogFile.set(params.changelogFileProvider)
            it.issueNumberPattern.set(params.issueNumberPattern)
            it.projectId.set(automationConfig.projectId)
            it.labelPattern.set(automationConfig.labelPattern)
            it.networkService.set(networkService)
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
