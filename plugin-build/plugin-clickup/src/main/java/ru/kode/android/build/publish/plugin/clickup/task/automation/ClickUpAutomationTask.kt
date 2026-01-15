package ru.kode.android.build.publish.plugin.clickup.task.automation

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
import ru.kode.android.build.publish.plugin.clickup.messages.issuesNotFoundMessage
import ru.kode.android.build.publish.plugin.clickup.service.network.ClickUpService
import ru.kode.android.build.publish.plugin.clickup.task.automation.work.AddFixVersionWork
import ru.kode.android.build.publish.plugin.clickup.task.automation.work.AddTagToTaskWork
import ru.kode.android.build.publish.plugin.core.enity.Tag
import ru.kode.android.build.publish.plugin.core.git.mapper.fromJson
import ru.kode.android.build.publish.plugin.core.logger.LoggerService
import javax.inject.Inject

/**
 * A Gradle task that automates ClickUp task management during the build process.
 *
 * This task is responsible for:
 * - Extracting issue numbers from the changelog using a specified pattern
 * - Adding version tags to ClickUp tasks mentioned in the changelog
 * - Updating custom fix version fields for tasks
 * - Processing tasks asynchronously using Gradle's Worker API
 *
 * The task is typically registered by [ClickUpTasksRegistrar] based on the build configuration.
 *
 * @see ClickUpTasksRegistrar For task registration
 * @see ClickUpService For the underlying network operations
 */
abstract class ClickUpAutomationTask
    @Inject
    constructor(
        private val workerExecutor: WorkerExecutor,
    ) : DefaultTask() {
        init {
            description = "Automates ClickUp task management during the build process"
            group = BasePlugin.BUILD_GROUP
        }

        /**
         * The network service used to communicate with the ClickUp API.
         *
         * This is an internal property that's injected when the task is created.
         */
        @get:Internal
        abstract val service: Property<ClickUpService>

        /**
         * The logger service used to log messages during task execution.
         *
         * This service is used to log messages related to the task execution.
         *
         * @see LoggerService
         */
        @get:ServiceReference
        abstract val loggerService: Property<LoggerService>

        /**
         * The name of the ClickUp workspace to operate on.
         *
         * This is the name of the ClickUp workspace where tasks are located.
         * It's used when determining the custom field ID for fix versions.
         */
        @get:Input
        @get:Option(
            option = "workspaceName",
            description = "Name of the ClickUp workspace to operate on",
        )
        abstract val workspaceName: Property<String>

        /**
         * A JSON file containing build tag information.
         *
         * This file is used to determine the current build version and number,
         * which are used when applying version tags or updating fix versions.
         */
        @get:InputFile
        @get:Option(
            option = "buildTagSnapshotFile",
            description = "JSON file containing build tag information",
        )
        abstract val buildTagSnapshotFile: RegularFileProperty

        /**
         * The changelog file containing commit messages and issue references.
         *
         * This file is parsed to extract issue numbers that match the [issueNumberPattern].
         * Each matching issue will be processed to add tags or update versions.
         */
        @get:InputFile
        @get:Option(
            option = "changelogFile",
            description = "Changelog file containing commit messages and issue references",
        )
        abstract val changelogFile: RegularFileProperty

        /**
         * Regular expression pattern used to extract issue numbers from the changelog.
         *
         * The pattern should include a capturing group that matches the issue number.
         * For example, if your issues are referenced as `#123`, the pattern could be `#(\\d+)`.
         *
         * The first capturing group will be used as the issue number when making API calls.
         */
        @get:Input
        @get:Option(
            option = "issueNumberPattern",
            description = "Regex pattern to extract issue numbers from changelog",
        )
        abstract val issueNumberPattern: Property<String>

        /**
         * Pattern used to format the fix version for ClickUp tasks.
         *
         * This pattern can include placeholders that will be replaced with actual values:
         * - `{0}` will be replaced with the build version
         * - `{1}` will be replaced with the build number
         *
         * Example: `"{0} ({1})"` might result in `"1.2.3 (456)"`
         *
         * If specified, [fixVersionFieldName] must also be provided.
         */
        @get:Input
        @get:Option(
            option = "fixVersionPattern",
            description = "Pattern to format fix versions for ClickUp tasks",
        )
        @get:Optional
        abstract val fixVersionPattern: Property<String>

        /**
         * The ID of the custom field in ClickUp where the fix version is stored.
         *
         * This is the identifier of the custom field in ClickUp where the version
         * (formatted according to [fixVersionPattern]) will be set.
         *
         * If specified, [fixVersionPattern] must also be provided.
         */
        @get:Input
        @get:Option(
            option = "fixVersionFieldName",
            description = "Name of the custom field where the fix version is stored",
        )
        @get:Optional
        abstract val fixVersionFieldName: Property<String>

        /**
         * The tag to be added to ClickUp tasks mentioned in the changelog.
         *
         * This tag will be added to all tasks that are found in the changelog.
         * It's typically used to mark which release a task was included in.
         *
         * If not specified, no tags will be added to tasks.
         */
        @get:Input
        @get:Option(
            option = "tagPattern",
            description = "Tag to be added to ClickUp tasks mentioned in the changelog",
        )
        @get:Optional
        abstract val tagPattern: Property<String>

        /**
         * The main task action that processes the changelog and updates ClickUp tasks.
         *
         * This method:
         * 1. Reads the build tag and changelog files
         * 2. Extracts issue numbers from the changelog using [issueNumberPattern]
         * 3. If no issues are found, logs a message and exits
         * 4. Otherwise, submits work items to update versions and/or add tags
         *
         * The actual work is performed asynchronously using Gradle's Worker API.
         */
        @TaskAction
        fun processClickUpTasks() {
            val currentBuildTag = fromJson(buildTagSnapshotFile.asFile.get()).current
            val changelog = changelogFile.asFile.get().readText()
            val issues =
                Regex(issueNumberPattern.get())
                    .findAll(changelog)
                    .mapTo(mutableSetOf()) { it.groupValues[0] }
            if (issues.isEmpty()) {
                loggerService.get().info(issuesNotFoundMessage())
            } else {
                val fixVersionFieldId =
                    service.flatMap { service ->
                        workspaceName
                            .zip(fixVersionFieldName) { workspaceName, fixVersionFieldName ->
                                service.getCustomFieldId(workspaceName, fixVersionFieldName)
                            }
                    }
                val workQueue: WorkQueue = workerExecutor.noIsolation()
                workQueue.submitUpdateVersionIfPresent(currentBuildTag, issues, fixVersionFieldId)
                workQueue.submitSetTagIfPresent(currentBuildTag, issues)
            }
        }

        /**
         * Submits work to update the fix version for the given issues, if configured.
         *
         * This method checks if both [fixVersionPattern] and [fixVersionFieldName] are set,
         * and if so, submits a work item to update the fix version for each issue.
         *
         * @param currentBuildTag The current build tag containing version and build number
         * @param issues The set of issue numbers to update
         */
        private fun WorkQueue.submitUpdateVersionIfPresent(
            currentBuildTag: Tag.Build,
            issues: Set<String>,
            fieldId: Provider<String>,
        ) {
            if (fixVersionPattern.isPresent && fixVersionFieldName.isPresent) {
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
                    parameters.fieldId.set(fieldId)
                    parameters.service.set(service)
                }
            }
        }

        /**
         * Submits work to add the configured tag to the given issues, if a tag is configured.
         *
         * This method checks if [tagPattern] is set, and if so, submits a work item
         * to add the tag to each issue.
         *
         * @param issues The set of issue numbers to tag
         */
        private fun WorkQueue.submitSetTagIfPresent(
            currentBuildTag: Tag.Build,
            issues: Set<String>,
        ) {
            if (tagPattern.isPresent) {
                val tagName =
                    tagPattern.map {
                        it.format(
                            currentBuildTag.buildVersion,
                            currentBuildTag.buildNumber,
                            currentBuildTag.buildVariant,
                        )
                    }
                submit(AddTagToTaskWork::class.java) { parameters ->
                    parameters.issues.set(issues)
                    parameters.tagName.set(tagName)
                    parameters.service.set(service)
                }
            }
        }
    }
