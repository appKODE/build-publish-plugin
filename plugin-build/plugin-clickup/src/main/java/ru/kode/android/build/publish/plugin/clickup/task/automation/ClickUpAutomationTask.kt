package ru.kode.android.build.publish.plugin.clickup.task.automation

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import ru.kode.android.build.publish.plugin.clickup.messages.issuesNotFoundMessage
import ru.kode.android.build.publish.plugin.clickup.service.network.ClickUpService
import ru.kode.android.build.publish.plugin.clickup.task.automation.work.AddFixVersionWork
import ru.kode.android.build.publish.plugin.clickup.task.automation.work.AddTagToTaskWork
import ru.kode.android.build.publish.plugin.core.entity.Tag
import ru.kode.android.build.publish.plugin.core.git.mapper.fromJson
import ru.kode.android.build.publish.plugin.core.logger.LoggerService
import javax.inject.Inject

/**
 * A Gradle task that automates ClickUp task management during the build process.
 *
 * This task is responsible for:
 * - Extracting issue numbers from the changelog using a specified pattern
 * - Routing each issue to its owning project (by custom-task-id prefix)
 * - Adding version tags and custom fix-version fields to the matching ClickUp tasks
 * - Processing tasks asynchronously using Gradle's Worker API
 *
 * The task is typically registered by [ClickUpTasksRegistrar] based on the build configuration.
 *
 * @see ClickUpTasksRegistrar For task registration
 * @see ClickUpService For the underlying network operations
 */
@DisableCachingByDefault(because = "Need to track changes each time")
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
         * The ClickUp build service for this variant, holding every configured account. A single service
         * (not a per-account reference) is handed to the Worker API, so each action selects its account
         * by [ClickUpProjectBinding.accountName]. Wired up automatically by the plugin.
         */
        @get:Internal
        abstract val service: Property<ClickUpService>

        /**
         * The logger service used to log messages during task execution.
         *
         * @see LoggerService
         */
        @get:Internal
        abstract val loggerService: Property<LoggerService>

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
        @get:PathSensitive(PathSensitivity.RELATIVE)
        abstract val buildTagSnapshotFile: RegularFileProperty

        /**
         * The changelog file containing commit messages and issue references.
         *
         * This file is parsed to extract issue numbers that match the [issuePatterns].
         * Each matching issue will be routed to its owning project and processed.
         */
        @get:InputFile
        @get:Option(
            option = "changelogFile",
            description = "Changelog file containing commit messages and issue references",
        )
        @get:PathSensitive(PathSensitivity.RELATIVE)
        abstract val changelogFile: RegularFileProperty

        /**
         * Regular expression patterns (one per configured changelog issue source) used to extract
         * ClickUp task ids from the changelog.
         *
         * Note: the task uses the full match (`MatchResult.groupValues[0]`) as the ClickUp task id.
         */
        @get:Input
        @get:Option(
            option = "issuePattern",
            description = "Regex pattern to extract issue numbers from changelog (repeatable)",
        )
        abstract val issuePatterns: ListProperty<String>

        /**
         * The ClickUp projects targeted by this automation run, resolved from the plugin DSL.
         *
         * Each binding carries its account name, workspace, custom-task-id prefix and effective
         * automation patterns.
         */
        @get:Nested
        abstract val projects: ListProperty<ClickUpProjectBinding>

        /**
         * The main task action that processes the changelog and updates ClickUp tasks.
         *
         * This method:
         * 1. Reads the build tag and changelog files
         * 2. Extracts issue numbers from the changelog using [issuePatterns]
         * 3. If no issues are found, logs a message and exits
         * 4. Otherwise, routes each issue to its owning project (by prefix) and submits work items to
         *    update versions and/or add tags
         *
         * The actual work is performed asynchronously using Gradle's Worker API.
         */
        @TaskAction
        fun processClickUpTasks() {
            val currentBuildTag = fromJson(buildTagSnapshotFile.asFile.get()).current
            val changelog = changelogFile.asFile.get().readText()
            val issues =
                issuePatterns.get()
                    .flatMapTo(mutableSetOf()) { pattern ->
                        Regex(pattern).findAll(changelog).map { it.value }.asIterable()
                    }
            if (issues.isEmpty()) {
                loggerService.get().info(issuesNotFoundMessage())
                return
            }

            val bindings = resolveBindings()
            val knownPrefixes = bindings.mapTo(mutableSetOf()) { it.taskIdPrefix }
            val issuesByPrefix = issues.groupBy { it.substringBefore("-").uppercase() }
            // Issues whose prefix matches no configured project are treated as native ClickUp task ids and
            // are applied against every targeted project, mirroring ClickUpIssueResolver's native-id
            // fallback. Without this, native ids (the common case, e.g. "86c72yxu4") would be dropped and
            // no tag/fix-version would ever be applied.
            val nativeIssues = issuesByPrefix.filterKeys { it !in knownPrefixes }.values.flatten().toSet()
            val workQueue: WorkQueue = workerExecutor.noIsolation()

            bindings.forEach { binding ->
                val bindingIssues = (issuesByPrefix[binding.taskIdPrefix].orEmpty() + nativeIssues).toSet()
                if (bindingIssues.isNotEmpty()) {
                    workQueue.applyAutomation(currentBuildTag, binding, bindingIssues)
                }
            }
        }

        /**
         * Resolves the list of project bindings to act on from the configured DSL bindings.
         */
        private fun resolveBindings(): List<ResolvedBinding> =
            projects.get().map { binding ->
                ResolvedBinding(
                    accountName = binding.accountName.get(),
                    workspaceName = binding.workspaceName.get(),
                    taskIdPrefix = binding.taskIdPrefix.get().uppercase(),
                    fixVersionPattern = binding.fixVersionPattern.orNull,
                    fixVersionFieldName = binding.fixVersionFieldName.orNull,
                    tagPattern = binding.tagPattern.orNull,
                )
            }

        /**
         * Submits the enabled automation actions (fix version, tag) for a single project against its
         * ClickUp account and issue subset. The task's single [service] is handed to every worker as a
         * build-service reference (never a resolved account), so it isolates cleanly; each action routes
         * to the project's account via [ResolvedBinding.accountName].
         */
        private fun WorkQueue.applyAutomation(
            currentBuildTag: Tag.Build,
            binding: ResolvedBinding,
            issues: Set<String>,
        ) {
            if (binding.fixVersionPattern != null && binding.fixVersionFieldName != null) {
                submit(AddFixVersionWork::class.java) { parameters ->
                    parameters.accountName.set(binding.accountName)
                    parameters.workspaceName.set(binding.workspaceName)
                    parameters.fieldName.set(binding.fixVersionFieldName)
                    parameters.version.set(binding.fixVersionPattern.formatWith(currentBuildTag))
                    parameters.issues.set(issues)
                    parameters.service.set(service)
                    parameters.loggerService.set(loggerService)
                }
            }
            binding.tagPattern?.let { pattern ->
                submit(AddTagToTaskWork::class.java) { parameters ->
                    parameters.accountName.set(binding.accountName)
                    parameters.tagName.set(pattern.formatWith(currentBuildTag))
                    parameters.issues.set(issues)
                    parameters.service.set(service)
                    parameters.loggerService.set(loggerService)
                }
            }
        }

        private fun String.formatWith(tag: Tag.Build) = format(tag.buildVersion, tag.buildNumber, tag.buildVariant)

        /**
         * A fully-resolved project to act on during a single automation run: its owning account name,
         * workspace, (upper-cased) task-id prefix and effective automation patterns.
         */
        private data class ResolvedBinding(
            val accountName: String,
            val workspaceName: String,
            val taskIdPrefix: String,
            val fixVersionPattern: String?,
            val fixVersionFieldName: String?,
            val tagPattern: String?,
        )
    }
