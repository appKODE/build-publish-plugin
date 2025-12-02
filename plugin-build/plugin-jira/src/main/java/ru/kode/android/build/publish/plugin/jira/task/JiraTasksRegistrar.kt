package ru.kode.android.build.publish.plugin.jira.task

import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.util.capitalizedName
import ru.kode.android.build.publish.plugin.core.util.getByNameOrCommon
import ru.kode.android.build.publish.plugin.jira.config.JiraAutomationConfig
import ru.kode.android.build.publish.plugin.jira.service.JiraServiceExtension
import ru.kode.android.build.publish.plugin.jira.task.automation.JiraAutomationTask

internal const val JIRA_AUTOMATION_TASK = "jiraAutomation"

/**
 * Registrar for Jira-related Gradle tasks.
 *
 * This object is responsible for registering and configuring all Jira-related tasks
 * in the Gradle build. It handles the creation of tasks for automating Jira workflows
 * during the build process.
 *
 * @see JiraAutomationTask For the task that performs Jira automation
 * @see JiraAutomationConfig For configuration options
 */
internal object JiraTasksRegistrar {
    /**
     * Registers a Jira automation task for the given project and configuration.
     *
     * This method creates and configures a [JiraAutomationTask] based on the provided
     * [JiraAutomationConfig] and [JiraAutomationTaskParams]. The task will be registered
     * only if at least one automation feature (label, fix version, or status transition) is enabled.
     *
     * @param project The Gradle project to register the task in
     * @param automationConfig The Jira automation configuration
     * @param params Parameters for the automation task
     *
     * @return A [TaskProvider] for the created task, or null if no automation features are enabled
     */
    internal fun registerAutomationTask(
        project: Project,
        automationConfig: JiraAutomationConfig,
        params: JiraAutomationTaskParams,
    ): TaskProvider<JiraAutomationTask>? {
        return project.registerJiraTasks(automationConfig, params)
    }
}

/**
 * Registers Jira automation tasks for the given project and build variant.
 *
 * This extension function configures a [JiraAutomationTask] with the provided
 * automation configuration and parameters. The task will be registered only if
 * at least one automation feature is enabled in the configuration.
 *
 * @receiver The Gradle project to register tasks in
 * @param automationConfig The Jira automation configuration
 * @param params Parameters for the automation task
 *
 * @return A [TaskProvider] for the created task, or null if no automation features are enabled
 */
private fun Project.registerJiraTasks(
    automationConfig: JiraAutomationConfig,
    params: JiraAutomationTaskParams,
): TaskProvider<JiraAutomationTask>? {
    return if (
        automationConfig.labelPattern.isPresent ||
        automationConfig.fixVersionPattern.isPresent ||
        automationConfig.resolvedStatusTransitionId.isPresent
    ) {
        val service =
            project.extensions
                .getByType(JiraServiceExtension::class.java)
                .services
                .get()
                .getByNameOrCommon(params.buildVariant.name)

        tasks.register(
            "$JIRA_AUTOMATION_TASK${params.buildVariant.capitalizedName()}",
            JiraAutomationTask::class.java,
        ) {
            it.buildTagFile.set(params.lastBuildTagFile)
            it.changelogFile.set(params.changelogFile)
            it.issueNumberPattern.set(params.issueNumberPattern)
            it.projectId.set(automationConfig.projectId)
            it.labelPattern.set(automationConfig.labelPattern)
            it.service.set(service)
            it.fixVersionPattern.set(automationConfig.fixVersionPattern)
            it.resolvedStatusTransitionId.set(automationConfig.resolvedStatusTransitionId)

            it.usesService(service)
        }
    } else {
        null
    }
}

/**
 * Parameters for configuring a Jira automation task.
 */
internal data class JiraAutomationTaskParams(
    /**
     * The build variant this task is associated with
     */
    val buildVariant: BuildVariant,
    /**
     * Provider for the pattern used to extract issue numbers from commit messages
     */
    val issueNumberPattern: Provider<String>,
    /**
     * Provider for the changelog file containing commit messages
     */
    val changelogFile: Provider<RegularFile>,
    /**
     * Provider for the file containing the last build tag
     */
    val lastBuildTagFile: Provider<RegularFile>,
)
