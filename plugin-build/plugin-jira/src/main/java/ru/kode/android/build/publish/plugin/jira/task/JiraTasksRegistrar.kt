package ru.kode.android.build.publish.plugin.jira.task

import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.util.capitalizedName
import ru.kode.android.build.publish.plugin.core.util.flatMapByNameOrCommon
import ru.kode.android.build.publish.plugin.jira.config.JiraAutomationConfig
import ru.kode.android.build.publish.plugin.jira.service.JiraServiceExtension
import ru.kode.android.build.publish.plugin.jira.task.automation.JiraAutomationTask

internal const val JIRA_AUTOMATION_TASK = "jiraAutomation"

internal object JiraTasksRegistrar {
    internal fun registerAutomationTask(
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
                .getByType(JiraServiceExtension::class.java)
                .networkServices
                .flatMapByNameOrCommon(params.buildVariant.name)

        tasks.register(
            "$JIRA_AUTOMATION_TASK${params.buildVariant.capitalizedName()}",
            JiraAutomationTask::class.java,
        ) {
            it.buildTagFile.set(params.lastBuildTagFile)
            it.changelogFile.set(params.changelogFile)
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

internal data class JiraAutomationTaskParams(
    val buildVariant: BuildVariant,
    val issueNumberPattern: Provider<String>,
    val changelogFile: Provider<RegularFile>,
    val lastBuildTagFile: Provider<RegularFile>,
)
