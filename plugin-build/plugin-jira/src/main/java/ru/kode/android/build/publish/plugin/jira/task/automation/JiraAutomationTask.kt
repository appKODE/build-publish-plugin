package ru.kode.android.build.publish.plugin.jira.task.automation

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import ru.kode.android.build.publish.plugin.core.enity.Tag
import ru.kode.android.build.publish.plugin.core.git.mapper.fromJson
import ru.kode.android.build.publish.plugin.core.logger.LoggerService
import ru.kode.android.build.publish.plugin.jira.messages.issuesNoFoundMessage
import ru.kode.android.build.publish.plugin.jira.service.network.JiraService
import ru.kode.android.build.publish.plugin.jira.task.automation.work.AddFixVersionWork
import ru.kode.android.build.publish.plugin.jira.task.automation.work.AddLabelWork
import ru.kode.android.build.publish.plugin.jira.task.automation.work.SetStatusWork
import javax.inject.Inject

/**
 * A Gradle task that automates Jira workflows during the build process.
 *
 * This task performs the following actions based on the changelog and configuration:
 * 1. Extracts Jira issue numbers from the changelog using a regex pattern
 * 2. For each found issue, it can (if configured):
 *    - Add a label based on the build tag
 *    - Set a fix version based on the build tag
 *    - Transition the issue to a resolved status
 *
 * @see JiraService For the underlying Jira API communication
 * @see JiraTasksRegistrar For how this task is registered and configured
 */
abstract class JiraAutomationTask
    @Inject
    constructor(
        private val workerExecutor: WorkerExecutor,
    ) : DefaultTask() {
        init {
            description = "Automates Jira workflows during the build process"
            group = BasePlugin.BUILD_GROUP
        }

        /**
         * The Jira network service used to communicate with the Jira API.
         *
         * This is an internal property that's automatically wired up by the plugin.
         */
        @get:Internal
        abstract val service: Property<JiraService>

        /**
         * The logger service used to log messages during the execution of the task.
         *
         * This service provides methods to log messages at different levels of severity.
         *
         * @see LoggerService
         */
        @get:ServiceReference
        abstract val loggerService: Property<LoggerService>

        /**
         * JSON file containing information about the current build tag.
         *
         * This file is used to extract version information for fix versions and labels.
         */
        @get:InputFile
        @get:Option(option = "buildTagSnapshotFile", description = "JSON file containing build tag information")
        abstract val buildTagSnapshotFile: RegularFileProperty

        /**
         * File containing the changelog for this build.
         *
         * The task will scan this file for Jira issue numbers using the configured pattern.
         */
        @get:InputFile
        @get:Option(option = "changelogFile", description = "File containing the changelog for this build")
        abstract val changelogFile: RegularFileProperty

        /**
         * Regular expression pattern used to extract Jira issue numbers from the changelog.
         *
         * The pattern should match a Jira issue key inside the changelog.
         * For example, if your Jira tasks are referenced as `PROJECT-123`, the pattern could be `[A-Z]+-\\d+`.
         *
         * Note: the task uses the full match (`MatchResult.groupValues[0]`) as the Jira issue key.
         */
        @get:Input
        @get:Option(
            option = "issueNumberPattern",
            description = "Regex pattern to extract Jira issue numbers from changelog",
        )
        abstract val issueNumberPattern: Property<String>

        /**
         * The Key of the Jira project to create versions in.
         *
         * This is only required if fix version automation is enabled.
         */
        @get:Input
        @get:Option(
            option = "projectKey",
            description = "Key of the Jira project for version management",
        )
        abstract val projectKey: Property<String>

        /**
         * Pattern for generating labels to add to Jira issues.
         *
         * This pattern is formatted using `String.format(...)` and receives:
         * - `buildVersion`
         * - `buildNumber`
         * - `buildVariant`
         *
         * If not specified, no labels will be added.
         */
        @get:Input
        @get:Option(
            option = "labelPattern",
            description = "Pattern for generating labels to add to Jira issues",
        )
        @get:Optional
        abstract val labelPattern: Property<String>

        /**
         * Pattern for generating fix version names to add to Jira issues.
         *
         * This pattern is formatted using `String.format(...)` and receives:
         * - `buildVersion`
         * - `buildNumber`
         * - `buildVariant`
         *
         * If not specified, no fix versions will be set.
         */
        @get:Input
        @get:Option(
            option = "fixVersionPattern",
            description = "Pattern for generating fix version names",
        )
        @get:Optional
        abstract val fixVersionPattern: Property<String>

        /**
         * The name of the status to mark issues as resolved.
         *
         * If specified, the task will submit work items to update the status of each issue to the
         * resolved status transition specified by [targetStatusName].
         */
        @get:Input
        @get:Option(
            option = "targetStatusName",
            description = "Name of the status to mark issues as resolved",
        )
        @get:Optional
        abstract val targetStatusName: Property<String>

        /**
         * Executes the Jira automation task.
         *
         * This method is called by Gradle when the task is executed. It:
         * 1. Reads the build tag and changelog
         * 2. Extracts Jira issue numbers from the changelog
         * 3. Submits work items for each automation action (labels, versions, status)
         */
        @TaskAction
        fun executeAutomation() {
            val currentBuildTag = fromJson(buildTagSnapshotFile.asFile.get()).current
            val changelog = changelogFile.asFile.get().readText()
            val issues =
                Regex(issueNumberPattern.get())
                    .findAll(changelog)
                    .mapTo(mutableSetOf()) { it.groupValues[0] }

            if (issues.isEmpty()) {
                loggerService.get().info(issuesNoFoundMessage())
            } else {
                val projectId =
                    service.flatMap { service ->
                        projectKey.map { projectKey ->
                            service.getProjectId(projectKey.uppercase())
                        }
                    }

                val workQueue: WorkQueue = workerExecutor.noIsolation()
                workQueue.submitUpdateLabelIfPresent(currentBuildTag, issues)
                workQueue.submitUpdateVersionIfPresent(currentBuildTag, issues, projectId)
                workQueue.submitUpdateStatusIfPresent(issues)
            }
        }

        /**
         * Submits work to update the status of the given issues, if a resolved status transition ID is configured.
         *
         * This method checks if [targetStatusName] is present, and if so, submits a work item to update the
         * status of each issue to the resolved status transition specified by [targetStatusName].
         *
         * @param issues The set of issue numbers to update
         */
        private fun WorkQueue.submitUpdateStatusIfPresent(issues: Set<String>) {
            if (targetStatusName.isPresent) {
                val statusTransitionId =
                    service.flatMap { service ->
                        projectKey
                            .zip(targetStatusName) { projectKey, statusName ->
                                service.getStatusTransitionId(projectKey, statusName, issues.toList())
                            }
                    }

                submit(SetStatusWork::class.java) { parameters ->
                    parameters.issues.set(issues)
                    parameters.statusTransitionId.set(statusTransitionId)
                    parameters.service.set(service)
                    parameters.loggerService.set(loggerService)
                }
            }
        }

        /**
         * Submits work to update the fix version for the given issues, if a version pattern is configured.
         *
         * This method checks if [fixVersionPattern] is set, and if so, generates a version name
         * from the current build tag and submits a work item to set the version for each issue.
         *
         * @param currentBuildTag The current build tag containing version and build number
         * @param issues The set of issue numbers to update
         */
        private fun WorkQueue.submitUpdateVersionIfPresent(
            currentBuildTag: Tag.Build,
            issues: Set<String>,
            projectId: Provider<Long>,
        ) {
            if (fixVersionPattern.isPresent) {
                val version =
                    fixVersionPattern.map {
                        it.format(
                            currentBuildTag.buildVersion,
                            currentBuildTag.buildNumber,
                            currentBuildTag.buildVariant,
                        )
                    }
                submit(AddFixVersionWork::class.java) { parameters ->
                    parameters.issues.set(issues)
                    parameters.version.set(version)
                    parameters.projectId.set(projectId)
                    parameters.service.set(service)
                }
            }
        }

        /**
         * Submits work to add the configured label to the given issues, if a label pattern is configured.
         *
         * This method checks if [labelPattern] is set, and if so, submits a work item
         * to add the label to each issue.
         *
         * @param currentBuildTag The current build tag containing version and build number
         * @param issues The set of issue numbers to label
         */
        private fun WorkQueue.submitUpdateLabelIfPresent(
            currentBuildTag: Tag.Build,
            issues: Set<String>,
        ) {
            if (labelPattern.isPresent) {
                val label =
                    labelPattern.map {
                        it.format(
                            currentBuildTag.buildVersion,
                            currentBuildTag.buildNumber,
                            currentBuildTag.buildVariant,
                        )
                    }
                submit(AddLabelWork::class.java) { parameters ->
                    parameters.issues.set(issues)
                    parameters.label.set(label)
                    parameters.service.set(service)
                }
            }
        }
    }
