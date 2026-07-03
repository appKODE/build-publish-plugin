package ru.kode.android.build.publish.plugin.jira.task.automation

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
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
import ru.kode.android.build.publish.plugin.core.enity.Tag
import ru.kode.android.build.publish.plugin.core.git.mapper.fromJson
import ru.kode.android.build.publish.plugin.core.logger.LoggerService
import ru.kode.android.build.publish.plugin.jira.messages.issuesNoFoundMessage
import ru.kode.android.build.publish.plugin.jira.messages.noIssuesForProjectMessage
import ru.kode.android.build.publish.plugin.jira.messages.unknownInstanceNameMessage
import ru.kode.android.build.publish.plugin.jira.messages.unmatchedIssuesMessage
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
@DisableCachingByDefault
abstract class JiraAutomationTask
    @Inject
    constructor(
        private val workerExecutor: WorkerExecutor,
        objectFactory: ObjectFactory,
    ) : DefaultTask() {
        init {
            description = "Automates Jira workflows during the build process"
            group = BasePlugin.BUILD_GROUP
        }

        /**
         * The Jira network services available to this task, keyed by `auth { }` configuration name.
         *
         * A project binding selects its service by [JiraProjectBinding.instanceName]; bindings without an
         * explicit auth name use [defaultService]. Wired up automatically by the plugin.
         */
        @get:Internal
        val services: MapProperty<String, JiraService> =
            objectFactory.mapProperty(String::class.java, JiraService::class.java)

        /**
         * The Jira service used by project bindings that do not declare an explicit `instanceName`
         * (the variant-matched / common service). Wired up automatically by the plugin.
         */
        @get:Internal
        abstract val defaultService: Property<JiraService>

        /**
         * The logger service used to log messages during the execution of the task.
         *
         * This service provides methods to log messages at different levels of severity.
         *
         * @see LoggerService
         */
        @get:Internal
        abstract val loggerService: Property<LoggerService>

        /**
         * JSON file containing information about the current build tag.
         *
         * This file is used to extract version information for fix versions and labels.
         */
        @get:InputFile
        @get:Option(option = "buildTagSnapshotFile", description = "JSON file containing build tag information")
        @get:PathSensitive(PathSensitivity.RELATIVE)
        abstract val buildTagSnapshotFile: RegularFileProperty

        /**
         * File containing the changelog for this build.
         *
         * The task will scan this file for Jira issue numbers using the configured pattern.
         */
        @get:InputFile
        @get:Option(option = "changelogFile", description = "File containing the changelog for this build")
        @get:PathSensitive(PathSensitivity.RELATIVE)
        abstract val changelogFile: RegularFileProperty

        /**
         * Regular expression patterns (one per configured changelog issue source) used to extract
         * Jira issue keys from the changelog.
         *
         * Each pattern should match a Jira issue key, e.g. `PROJECT-\\d+` or `[A-Z]+-\\d+`. The full
         * match is used as the issue key; matches from all patterns are unioned.
         */
        @get:Input
        @get:Option(
            option = "issuePattern",
            description = "Regex pattern to extract Jira issue keys from changelog (repeatable)",
        )
        abstract val issuePatterns: ListProperty<String>

        /**
         * The Jira projects targeted by this automation run, resolved from the plugin DSL.
         *
         * Each binding carries its project key, the Jira instance to use (`instanceName`) and its
         * automation patterns.
         */
        @get:Nested
        abstract val projects: ListProperty<JiraProjectBinding>

        /**
         * Executes the Jira automation task.
         *
         * This method is called by Gradle when the task is executed. It:
         * 1. Reads the build tag and changelog
         * 2. Extracts Jira issue numbers from the changelog
         * 3. Routes each issue to its owning project (by issue-key prefix)
         * 4. Submits work items for each automation action (labels, versions, status) per project
         */
        @TaskAction
        fun executeAutomation() {
            val currentBuildTag = fromJson(buildTagSnapshotFile.asFile.get()).current
            val changelog = changelogFile.asFile.get().readText()
            val issues =
                issuePatterns.get()
                    .flatMapTo(mutableSetOf()) { pattern ->
                        Regex(pattern).findAll(changelog).map { it.value }.asIterable()
                    }

            if (issues.isEmpty()) {
                loggerService.get().info(issuesNoFoundMessage())
                return
            }

            val resolvedProjects = resolveProjects()
            val issuesByProjectKey = issues.groupBy { it.substringBefore("-").uppercase() }
            val matchedIssues = mutableSetOf<String>()
            val workQueue: WorkQueue = workerExecutor.noIsolation()

            resolvedProjects.forEach { project ->
                val projectIssues = issuesByProjectKey[project.projectKey].orEmpty().toSet()
                if (projectIssues.isEmpty()) {
                    loggerService.get().info(noIssuesForProjectMessage(project.projectKey))
                    return@forEach
                }
                matchedIssues += projectIssues
                workQueue.applyAutomation(currentBuildTag, project, projectIssues)
            }

            val unmatchedIssues = issues - matchedIssues
            if (unmatchedIssues.isNotEmpty()) {
                loggerService.get().info(
                    unmatchedIssuesMessage(unmatchedIssues, resolvedProjects.map { it.projectKey }),
                )
            }
        }

        /**
         * Resolves the list of projects to act on from the configured project bindings.
         */
        private fun resolveProjects(): List<ResolvedProject> {
            return projects.get().map { binding ->
                ResolvedProject(
                    projectKey = binding.projectKey.get().uppercase(),
                    service = resolveService(binding.instanceName.orNull),
                    labelPattern = binding.labelPattern.orNull,
                    fixVersionPattern = binding.fixVersionPattern.orNull,
                    targetStatusName = binding.targetStatusName.orNull,
                )
            }
        }

        /**
         * Resolves a [JiraService] provider for the given [instanceName], falling back to the
         * variant-matched / common service when no explicit name is provided.
         *
         * A provider (rather than the resolved service) is returned so it can be handed to the Worker
         * API as a build-service reference: passing a resolved [JiraService] instance as a work
         * parameter fails isolation ("Could not serialize value of type JiraService").
         */
        private fun resolveService(instanceName: String?): Provider<JiraService> {
            if (instanceName == null) return defaultService
            val available = services.get()
            if (instanceName !in available) {
                throw GradleException(unknownInstanceNameMessage(instanceName, available.keys))
            }
            return services.getting(instanceName)
        }

        /**
         * Submits the enabled automation actions (label, fix version, status transition) for a single
         * project against its own Jira service and issue subset.
         */
        private fun WorkQueue.applyAutomation(
            currentBuildTag: Tag.Build,
            project: ResolvedProject,
            issues: Set<String>,
        ) {
            project.labelPattern?.let { pattern ->
                submit(AddLabelWork::class.java) { parameters ->
                    parameters.issues.set(issues)
                    parameters.label.set(pattern.formatWith(currentBuildTag))
                    parameters.service.set(project.service)
                }
            }
            project.fixVersionPattern?.let { pattern ->
                val projectId = project.service.get().getProjectId(project.projectKey)
                submit(AddFixVersionWork::class.java) { parameters ->
                    parameters.issues.set(issues)
                    parameters.version.set(pattern.formatWith(currentBuildTag))
                    parameters.projectId.set(projectId)
                    parameters.service.set(project.service)
                }
            }
            project.targetStatusName?.let { statusName ->
                val statusTransitionId =
                    project.service.get().getStatusTransitionId(project.projectKey, statusName, issues.toList())
                submit(SetStatusWork::class.java) { parameters ->
                    parameters.issues.set(issues)
                    parameters.statusTransitionId.set(statusTransitionId)
                    parameters.service.set(project.service)
                    parameters.loggerService.set(loggerService)
                }
            }
        }

        private fun String.formatWith(tag: Tag.Build) = format(tag.buildVersion, tag.buildNumber, tag.buildVariant)

        /**
         * A fully-resolved project to act on during a single automation run: its (upper-cased)
         * project key, the Jira service to use, and its effective automation patterns.
         */
        private data class ResolvedProject(
            val projectKey: String,
            val service: Provider<JiraService>,
            val labelPattern: String?,
            val fixVersionPattern: String?,
            val targetStatusName: String?,
        )
    }
