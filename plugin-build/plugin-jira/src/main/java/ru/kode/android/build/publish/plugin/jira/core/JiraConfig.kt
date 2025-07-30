package ru.kode.android.build.publish.plugin.jira.core

import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskContainer
import ru.kode.android.build.publish.plugin.core.enity.BuildVariant
import ru.kode.android.build.publish.plugin.core.util.capitalizedName
import ru.kode.android.build.publish.plugin.jira.task.JiraAutomationTask

internal const val JIRA_AUTOMATION_TASK = "jiraAutomation"

interface JiraConfig {
    val name: String

    @get:Input
    val authUsername: Property<String>

    @get:Input
    val authPassword: Property<String>

    @get:Input
    val baseUrl: Property<String>

    @get:Input
    val projectId: Property<Long>

    @get:Input
    @get:Optional
    val labelPattern: Property<String>

    @get:Input
    @get:Optional
    val fixVersionPattern: Property<String>

    @get:Input
    @get:Optional
    val resolvedStatusTransitionId: Property<String>

    fun registerAutomationTask(
        project: Project,
        params: JiraAutomationTaskParams,
    ) {
        project.tasks.registerJiraTasks(this, params)
    }
}

private fun TaskContainer.registerJiraTasks(
    config: JiraConfig,
    params: JiraAutomationTaskParams,
) {
    if (
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
    }
}

data class JiraAutomationTaskParams(
    val buildVariant: BuildVariant,
    val issueNumberPattern: Provider<String>,
    val changelogFileProvider: Provider<RegularFile>,
    val tagBuildProvider: Provider<RegularFile>,
)
