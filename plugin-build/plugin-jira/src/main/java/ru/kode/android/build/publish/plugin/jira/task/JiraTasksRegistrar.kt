package ru.kode.android.build.publish.plugin.jira.task

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.logger.LoggerServiceExtension
import ru.kode.android.build.publish.plugin.core.task.GenerateChangelogTaskOutput
import ru.kode.android.build.publish.plugin.core.task.GetLastTagSnapshotTaskOutput
import ru.kode.android.build.publish.plugin.core.task.TaskNames
import ru.kode.android.build.publish.plugin.core.util.capitalizedName
import ru.kode.android.build.publish.plugin.jira.config.JiraAuthConfig
import ru.kode.android.build.publish.plugin.jira.config.JiraAutomationConfig
import ru.kode.android.build.publish.plugin.jira.messages.unknownInstanceNameMessage
import ru.kode.android.build.publish.plugin.jira.messages.unknownProjectNameMessage
import ru.kode.android.build.publish.plugin.jira.service.network.JiraService
import ru.kode.android.build.publish.plugin.jira.task.automation.JiraAutomationTask
import ru.kode.android.build.publish.plugin.jira.task.automation.JiraProjectBinding

/**
 * Registrar for Jira-related Gradle tasks.
 *
 * @see JiraAutomationTask For the task that performs Jira automation
 * @see JiraAutomationConfig For configuration options
 */
internal object JiraTasksRegistrar {
    /**
     * Registers a [JiraAutomationTask] for the given automation configuration, resolving its project
     * selections against the shared registry declared on [authConfig]. Returns `null` when no automation
     * action is enabled.
     */
    internal fun registerAutomationTask(
        project: Project,
        authConfig: JiraAuthConfig,
        automationConfig: JiraAutomationConfig,
        service: Provider<JiraService>,
        params: JiraAutomationTaskParams,
    ): TaskProvider<JiraAutomationTask>? {
        return project.registerJiraTasks(authConfig, automationConfig, service, params)
    }
}

private fun Project.registerJiraTasks(
    authConfig: JiraAuthConfig,
    automationConfig: JiraAutomationConfig,
    service: Provider<JiraService>,
    params: JiraAutomationTaskParams,
): TaskProvider<JiraAutomationTask>? {
    val bindings = resolveProjectBindings(authConfig, automationConfig)

    val hasAnyAction =
        bindings.any { binding ->
            binding.labelPattern.isPresent ||
                binding.fixVersionPattern.isPresent ||
                binding.targetStatusName.isPresent
        }
    if (!hasAnyAction) return null

    val loggerService =
        project.extensions
            .getByType(LoggerServiceExtension::class.java)
            .service

    return tasks.register(
        "${TaskNames.Jira.AUTOMATION_PREFIX}${params.buildVariant.capitalizedName()}",
        JiraAutomationTask::class.java,
    ) {
        it.buildTagSnapshotFile.set(params.buildTagSnapshotProvider.flatMap { it.buildTagSnapshotFile })
        it.changelogFile.set(params.changelogFileProvider.flatMap { it.changelogFile })
        it.issuePatterns.set(params.issuePatterns)
        it.projects.set(bindings)
        it.service.set(service)
        it.loggerService.set(loggerService)

        it.usesService(service)
        it.usesService(loggerService)

        it.dependsOn(params.buildTagSnapshotProvider, params.changelogFileProvider)
    }
}

/**
 * Builds the list of [JiraProjectBinding]s from the automation rule's `targetInstance` selections. Each
 * selected project's key/instance come from the registry declared on [authConfig], its automation
 * patterns from the per-project override folded over the automation-level defaults. Unknown instance or
 * project names fail fast at configuration time.
 */
private fun Project.resolveProjectBindings(
    authConfig: JiraAuthConfig,
    automationConfig: JiraAutomationConfig,
): List<JiraProjectBinding> {
    val bindings = mutableListOf<JiraProjectBinding>()
    automationConfig.selectionsConfig.selections.forEach { selection ->
        val instanceName = selection.name
        val instanceConfig =
            authConfig.instances.findByName(instanceName)
                ?: throw GradleException(unknownInstanceNameMessage(instanceName, authConfig.instances.names))
        selection.projectNames.get().forEach { projectName ->
            val projectDef =
                instanceConfig.projects.findByName(projectName)
                    ?: throw GradleException(
                        unknownProjectNameMessage(projectName, instanceName, instanceConfig.projects.names),
                    )
            val override = selection.projectOverrides.findByName(projectName)
            bindings +=
                objects.newInstance(JiraProjectBinding::class.java).apply {
                    this.projectKey.set(projectDef.projectKey)
                    this.instanceName.set(instanceName)
                    this.labelPattern.set(
                        override?.labelPattern?.orElse(automationConfig.labelPattern)
                            ?: automationConfig.labelPattern,
                    )
                    this.fixVersionPattern.set(
                        override?.fixVersionPattern?.orElse(automationConfig.fixVersionPattern)
                            ?: automationConfig.fixVersionPattern,
                    )
                    this.targetStatusName.set(
                        override?.targetStatusName?.orElse(automationConfig.targetStatusName)
                            ?: automationConfig.targetStatusName,
                    )
                }
        }
    }
    return bindings
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
     * Provider for the patterns (one per changelog issue source) used to extract issue keys
     */
    val issuePatterns: Provider<List<String>>,
    /**
     * Provider for the changelog file containing commit messages
     */
    val changelogFileProvider: TaskProvider<out GenerateChangelogTaskOutput>,
    /**
     * Provider for the file containing the last build tag
     */
    val buildTagSnapshotProvider: Provider<out GetLastTagSnapshotTaskOutput>,
)
