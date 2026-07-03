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
import ru.kode.android.build.publish.plugin.core.util.COMMON_CONTAINER_NAME
import ru.kode.android.build.publish.plugin.core.util.capitalizedName
import ru.kode.android.build.publish.plugin.jira.config.JiraAutomationConfig
import ru.kode.android.build.publish.plugin.jira.messages.duplicateProjectKeyMessage
import ru.kode.android.build.publish.plugin.jira.messages.unknownInstanceNameMessage
import ru.kode.android.build.publish.plugin.jira.service.JiraServiceExtension
import ru.kode.android.build.publish.plugin.jira.task.automation.JiraAutomationTask
import ru.kode.android.build.publish.plugin.jira.task.automation.JiraProjectBinding

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
    val bindings = resolveProjectBindings(automationConfig)

    val hasAnyAction =
        bindings.any { binding ->
            binding.labelPattern.isPresent ||
                binding.fixVersionPattern.isPresent ||
                binding.targetStatusName.isPresent
        }
    if (!hasAnyAction) return null

    val services =
        project.extensions
            .getByType(JiraServiceExtension::class.java)
            .services
            .get()
    validateInstanceNames(bindings, services.keys)

    // A project without an explicit `instanceName` falls back to the instance named `default`, or to
    // the single declared instance when there is only one.
    val defaultService = services[COMMON_CONTAINER_NAME] ?: services.values.first()
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
        it.defaultService.set(defaultService)
        it.loggerService.set(loggerService)

        services.forEach { (name, provider) ->
            it.services.put(name, provider)
            it.usesService(provider)
        }
        it.usesService(defaultService)
        it.usesService(loggerService)

        it.dependsOn(params.buildTagSnapshotProvider, params.changelogFileProvider)
    }
}

/**
 * Builds the list of [JiraProjectBinding]s to act on for an automation config: one binding per
 * declared project, each carrying its own project key, optional Jira instance (`instanceName`) and
 * automation patterns.
 */
private fun Project.resolveProjectBindings(automationConfig: JiraAutomationConfig): List<JiraProjectBinding> {
    val bindings =
        automationConfig.projectsConfig.projects.map { projectConfig ->
            objects.newInstance(JiraProjectBinding::class.java).apply {
                projectKey.set(projectConfig.projectKey)
                instanceName.set(projectConfig.instanceName)
                labelPattern.set(projectConfig.labelPattern)
                fixVersionPattern.set(projectConfig.fixVersionPattern)
                targetStatusName.set(projectConfig.targetStatusName)
            }
        }
    validateUniqueProjectKeys(bindings)
    return bindings
}

/** Fails fast when a project references an `instanceName` that has no matching auth configuration. */
private fun validateInstanceNames(
    bindings: List<JiraProjectBinding>,
    availableInstanceNames: Set<String>,
) {
    bindings.forEach { binding ->
        val instanceName = binding.instanceName.orNull
        if (instanceName != null && instanceName !in availableInstanceNames) {
            throw GradleException(unknownInstanceNameMessage(instanceName, availableInstanceNames))
        }
    }
}

/** Fails fast when two projects share the same key, which makes prefix-based routing ambiguous. */
private fun validateUniqueProjectKeys(bindings: List<JiraProjectBinding>) {
    val seen = mutableSetOf<String>()
    bindings.forEach { binding ->
        val key = binding.projectKey.orNull?.uppercase() ?: return@forEach
        if (!seen.add(key)) {
            throw GradleException(duplicateProjectKeyMessage(key))
        }
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
