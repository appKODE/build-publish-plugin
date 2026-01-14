package ru.kode.android.build.publish.plugin.clickup.task

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import ru.kode.android.build.publish.plugin.clickup.config.ClickUpAutomationConfig
import ru.kode.android.build.publish.plugin.clickup.messages.propertiesNotAppliedMessage
import ru.kode.android.build.publish.plugin.clickup.service.ClickUpServiceExtension
import ru.kode.android.build.publish.plugin.clickup.task.automation.ClickUpAutomationTask
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.task.GenerateChangelogTaskOutput
import ru.kode.android.build.publish.plugin.core.task.GetLastTagTaskOutput
import ru.kode.android.build.publish.plugin.core.util.capitalizedName
import ru.kode.android.build.publish.plugin.core.util.getByNameOrCommon

internal const val CLICK_UP_AUTOMATION_TASK = "clickUpAutomation"

/**
 * Registrar for ClickUp-related Gradle tasks.
 *
 * This object is responsible for registering and configuring ClickUp automation tasks
 * in the Gradle build. It handles the creation of tasks based on the provided
 * configuration and ensures proper dependencies are set up.
 */
internal object ClickUpTasksRegistrar {
    /**
     * Registers the ClickUp automation task for the given project and configuration.
     *
     * This method creates a [ClickUpAutomationTask] with the provided configuration
     * and parameters. The task will be registered with a name based on the build variant.
     *
     * @param project The Gradle project to register the task in
     * @param automationConfig The automation configuration to use for the task
     * @param params Parameters for configuring the task
     *
     * @return A [TaskProvider] for the registered task, or null if no task was created
     *   (which happens when neither fix version nor tag name is configured)
     * @throws GradleException If the fix version configuration is invalid
     */
    fun registerAutomationTask(
        project: Project,
        automationConfig: ClickUpAutomationConfig,
        params: ClickUpAutomationTaskParams,
    ): TaskProvider<ClickUpAutomationTask>? {
        return project.registerClickUpTasks(automationConfig, params)
    }
}

/**
 * Registers ClickUp automation tasks for the given project and configuration.
 *
 * This extension function handles the actual task registration and configuration.
 * It validates the configuration and creates the task if either fix version or
 * tag name is specified in the configuration.
 *
 * @receiver The Gradle project to register the task in
 * @param automationConfig The automation configuration to use
 * @param params Parameters for configuring the task
 *
 * @return A [TaskProvider] for the registered task, or null if no task was created
 * @throws GradleException If the fix version configuration is invalid
 */
private fun Project.registerClickUpTasks(
    automationConfig: ClickUpAutomationConfig,
    params: ClickUpAutomationTaskParams,
): TaskProvider<ClickUpAutomationTask>? {
    val fixVersionIsPresent =
        automationConfig.fixVersionPattern.isPresent && automationConfig.fixVersionFieldName.isPresent
    val hasMissingFixVersionProperties =
        automationConfig.fixVersionPattern.isPresent || automationConfig.fixVersionFieldName.isPresent

    if (!fixVersionIsPresent && hasMissingFixVersionProperties) {
        throw GradleException(propertiesNotAppliedMessage())
    }

    val service =
        project.extensions
            .getByType(ClickUpServiceExtension::class.java)
            .services
            .get()
            .getByNameOrCommon(params.buildVariant.name)

    return if (fixVersionIsPresent || automationConfig.tagPattern.isPresent) {
        tasks.register(
            "$CLICK_UP_AUTOMATION_TASK${params.buildVariant.capitalizedName()}",
            ClickUpAutomationTask::class.java,
        ) {
            it.workspaceName.set(automationConfig.workspaceName)
            it.buildTagFile.set(params.lastBuildTagFileProvider.flatMap { it.tagBuildFile })
            it.changelogFile.set(params.changelogFileProvider.flatMap { it.changelogFile })
            it.issueNumberPattern.set(params.issueNumberPattern)
            it.fixVersionPattern.set(automationConfig.fixVersionPattern)
            it.fixVersionFieldName.set(automationConfig.fixVersionFieldName)
            it.tagPattern.set(automationConfig.tagPattern)
            it.service.set(service)

            it.dependsOn(params.lastBuildTagFileProvider, params.changelogFileProvider)
        }
    } else {
        null
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
     * A pattern used to extract issue numbers from commit messages
     */
    val issueNumberPattern: Provider<String>,
    /**
     * The file containing the changelog for this build
     */
    val changelogFileProvider: TaskProvider<out GenerateChangelogTaskOutput>,
    /**
     * A file used to store the last build tag for change detection
     */
    val lastBuildTagFileProvider: Provider<out GetLastTagTaskOutput>,
)
