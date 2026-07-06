package ru.kode.android.build.publish.plugin.clickup.task

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import ru.kode.android.build.publish.plugin.clickup.config.ClickUpAuthConfig
import ru.kode.android.build.publish.plugin.clickup.config.ClickUpAutomationConfig
import ru.kode.android.build.publish.plugin.clickup.messages.propertiesNotAppliedMessage
import ru.kode.android.build.publish.plugin.clickup.messages.unknownAccountNameMessage
import ru.kode.android.build.publish.plugin.clickup.messages.unknownProjectNameMessage
import ru.kode.android.build.publish.plugin.clickup.service.ClickUpServiceExtension
import ru.kode.android.build.publish.plugin.clickup.task.automation.ClickUpAutomationTask
import ru.kode.android.build.publish.plugin.clickup.task.automation.ClickUpProjectBinding
import ru.kode.android.build.publish.plugin.core.entity.BuildVariant
import ru.kode.android.build.publish.plugin.core.logger.LoggerServiceExtension
import ru.kode.android.build.publish.plugin.core.task.GenerateChangelogTaskOutput
import ru.kode.android.build.publish.plugin.core.task.GetLastTagSnapshotTaskOutput
import ru.kode.android.build.publish.plugin.core.task.TaskNames
import ru.kode.android.build.publish.plugin.core.util.capitalizedName
import ru.kode.android.build.publish.plugin.core.util.getByNameOrCommon

/**
 * Registrar for ClickUp-related Gradle tasks.
 *
 * This object is responsible for registering and configuring ClickUp automation tasks
 * in the Gradle build. It resolves the automation rule's per-account project selections against the
 * shared registry declared on [ClickUpAuthConfig] and registers one task per build variant carrying
 * the resolved list of project bindings.
 */
internal object ClickUpTasksRegistrar {
    /**
     * Registers the ClickUp automation task for the given project and configuration.
     *
     * This method resolves the [automationConfig] project selections against the registry declared on
     * [authConfig] and creates a [ClickUpAutomationTask] carrying the resulting bindings. The task will
     * be registered with a name based on the build variant.
     *
     * @param project The Gradle project to register the task in
     * @param authConfig The auth config holding the account/project registry
     * @param automationConfig The automation configuration to use for the task
     * @param params Parameters for configuring the task
     *
     * @return A [TaskProvider] for the registered task, or null if no automation action is enabled
     * @throws GradleException If a fix-version configuration is partially specified, or an unknown
     *   account/project name is selected
     */
    fun registerAutomationTask(
        project: Project,
        authConfig: ClickUpAuthConfig,
        automationConfig: ClickUpAutomationConfig,
        params: ClickUpAutomationTaskParams,
    ): TaskProvider<ClickUpAutomationTask>? {
        return project.registerClickUpTasks(authConfig, automationConfig, params)
    }
}

/**
 * Registers ClickUp automation tasks for the given project and configuration.
 *
 * This extension function resolves the project bindings and creates the task when at least one
 * automation action (fix version or tag) is enabled across the selected projects.
 *
 * @receiver The Gradle project to register the task in
 * @param authConfig The auth config holding the account/project registry
 * @param automationConfig The automation configuration to use
 * @param params Parameters for configuring the task
 *
 * @return A [TaskProvider] for the registered task, or null if no automation action is enabled
 * @throws GradleException If a fix-version configuration is partially specified
 */
private fun Project.registerClickUpTasks(
    authConfig: ClickUpAuthConfig,
    automationConfig: ClickUpAutomationConfig,
    params: ClickUpAutomationTaskParams,
): TaskProvider<ClickUpAutomationTask>? {
    val bindings = resolveProjectBindings(authConfig, automationConfig)

    bindings.forEach { binding ->
        val fixVersionComplete =
            binding.fixVersionPattern.isPresent && binding.fixVersionFieldName.isPresent
        val fixVersionPartial =
            binding.fixVersionPattern.isPresent || binding.fixVersionFieldName.isPresent
        if (!fixVersionComplete && fixVersionPartial) {
            throw GradleException(propertiesNotAppliedMessage())
        }
    }

    val hasAnyAction =
        bindings.any { binding ->
            (binding.fixVersionPattern.isPresent && binding.fixVersionFieldName.isPresent) ||
                binding.tagPattern.isPresent
        }
    if (!hasAnyAction) return null

    val service =
        project.extensions
            .getByType(ClickUpServiceExtension::class.java)
            .services
            .get()
            .getByNameOrCommon(params.buildVariant.name)
    val loggerService =
        project.extensions
            .getByType(LoggerServiceExtension::class.java)
            .service

    return tasks.register(
        "${TaskNames.ClickUp.AUTOMATION_PREFIX}${params.buildVariant.capitalizedName()}",
        ClickUpAutomationTask::class.java,
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
 * Builds the list of [ClickUpProjectBinding]s from the automation rule's `targetAccount` selections.
 * Each selected project's workspace/prefix come from the registry declared on [authConfig], its
 * automation patterns from the per-project override folded over the automation-level defaults. Unknown
 * account or project names fail fast at configuration time.
 */
private fun Project.resolveProjectBindings(
    authConfig: ClickUpAuthConfig,
    automationConfig: ClickUpAutomationConfig,
): List<ClickUpProjectBinding> =
    automationConfig.selectionsConfig.selections.flatMap { selection ->
        val accountName = selection.name
        val accountConfig =
            authConfig.accounts.findByName(accountName)
                ?: throw GradleException(unknownAccountNameMessage(accountName, authConfig.accounts.names))
        selection.projectNames.get().map { projectName ->
            val projectDef =
                accountConfig.projects.findByName(projectName)
                    ?: throw GradleException(
                        unknownProjectNameMessage(projectName, accountName, accountConfig.projects.names),
                    )
            val override = selection.projectOverrides.findByName(projectName)
            objects.newInstance(ClickUpProjectBinding::class.java).apply {
                this.accountName.set(accountName)
                this.workspaceName.set(projectDef.workspaceName)
                this.taskIdPrefix.set(projectDef.taskIdPrefix)
                this.fixVersionPattern.set(
                    override?.fixVersionPattern?.orElse(automationConfig.fixVersionPattern)
                        ?: automationConfig.fixVersionPattern,
                )
                this.fixVersionFieldName.set(
                    override?.fixVersionFieldName?.orElse(automationConfig.fixVersionFieldName)
                        ?: automationConfig.fixVersionFieldName,
                )
                this.tagPattern.set(
                    override?.tagPattern?.orElse(automationConfig.tagPattern)
                        ?: automationConfig.tagPattern,
                )
            }
        }
    }

/**
 * Parameters for configuring a ClickUp automation task.
 *
 * This data class holds all the necessary parameters to configure a [ClickUpAutomationTask].
 */
internal data class ClickUpAutomationTaskParams(
    /**
     * The build variant this task is associated with
     */
    val buildVariant: BuildVariant,
    /**
     * Patterns (one per changelog issue source) used to extract issue numbers from commit messages
     */
    val issuePatterns: Provider<List<String>>,
    /**
     * The file containing the changelog for this build
     */
    val changelogFileProvider: TaskProvider<out GenerateChangelogTaskOutput>,
    /**
     * A file used to store the last build tag for change detection
     */
    val buildTagSnapshotProvider: Provider<out GetLastTagSnapshotTaskOutput>,
)
