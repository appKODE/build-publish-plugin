package ru.kode.android.build.publish.plugin.jira.task.automation

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Property
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
import ru.kode.android.build.publish.plugin.jira.service.network.JiraNetworkService
import ru.kode.android.build.publish.plugin.jira.task.automation.work.AddFixVersionWork
import ru.kode.android.build.publish.plugin.jira.task.automation.work.AddLabelWork
import ru.kode.android.build.publish.plugin.jira.task.automation.work.SetStatusWork
import javax.inject.Inject

abstract class JiraAutomationTask
    @Inject
    constructor(
        private val workerExecutor: WorkerExecutor,
    ) : DefaultTask() {
        init {
            description = "Task to automate jira statuses"
            group = BasePlugin.BUILD_GROUP
        }

        @get:Internal
        abstract val networkService: Property<JiraNetworkService>

        @get:InputFile
        @get:Option(option = "buildTagFile", description = "Json contains info about tag build")
        abstract val buildTagFile: RegularFileProperty

        @get:InputFile
        @get:Option(option = "changelogFile", description = "File with saved changelog")
        abstract val changelogFile: RegularFileProperty

        @get:Input
        @get:Option(
            option = "issueNumberPattern",
            description = "How task number formatted",
        )
        abstract val issueNumberPattern: Property<String>

        @get:Input
        @get:Option(
            option = "projectId",
            description = "Project id in jira task tracker",
        )
        abstract val projectId: Property<Long>

        @get:Input
        @get:Option(
            option = "labelPattern",
            description = "How label is should be formatted",
        )
        @get:Optional
        abstract val labelPattern: Property<String>

        @get:Input
        @get:Option(
            option = "fixVersionPattern",
            description = "How fix version should be formatted",
        )
        @get:Optional
        abstract val fixVersionPattern: Property<String>

        @get:Input
        @get:Option(
            option = "resolvedStatusId",
            description = "Id of resolved task status",
        )
        @get:Optional
        abstract val resolvedStatusTransitionId: Property<String>

        @TaskAction
        fun upload() {
            val currentBuildTag = fromJson(buildTagFile.asFile.get())
            val changelog = changelogFile.asFile.get().readText()
            val issues =
                Regex(issueNumberPattern.get())
                    .findAll(changelog)
                    .mapTo(mutableSetOf()) { it.groupValues[0] }

            if (issues.isEmpty()) {
                logger.info("issues not found in the changelog, nothing will change")
            } else {
                val workQueue: WorkQueue = workerExecutor.noIsolation()
                workQueue.submitUpdateLabelIfPresent(currentBuildTag, issues)
                workQueue.submitUpdateVersionIfPresent(currentBuildTag, issues)
                workQueue.submitUpdateStatusIfPresent(issues)
            }
        }

        private fun WorkQueue.submitUpdateStatusIfPresent(issues: MutableSet<String>) {
            if (resolvedStatusTransitionId.isPresent) {
                submit(SetStatusWork::class.java) { parameters ->
                    parameters.issues.set(issues)
                    parameters.statusTransitionId.set(resolvedStatusTransitionId)
                    parameters.networkService.set(networkService)
                }
            }
        }

        private fun WorkQueue.submitUpdateVersionIfPresent(
            currentBuildTag: Tag.Build,
            issues: MutableSet<String>,
        ) {
            if (fixVersionPattern.isPresent) {
                val version =
                    fixVersionPattern.get()
                        .format(
                            currentBuildTag.buildVersion,
                            currentBuildTag.buildNumber,
                            currentBuildTag.buildVariant,
                        )
                submit(AddFixVersionWork::class.java) { parameters ->
                    parameters.issues.set(issues)
                    parameters.version.set(version)
                    parameters.projectId.set(projectId)
                    parameters.networkService.set(networkService)
                }
            }
        }

        private fun WorkQueue.submitUpdateLabelIfPresent(
            currentBuildTag: Tag.Build,
            issues: MutableSet<String>,
        ) {
            if (labelPattern.isPresent) {
                val label =
                    labelPattern.get()
                        .format(
                            currentBuildTag.buildVersion,
                            currentBuildTag.buildNumber,
                            currentBuildTag.buildVariant,
                        )
                submit(AddLabelWork::class.java) { parameters ->
                    parameters.issues.set(issues)
                    parameters.label.set(label)
                    parameters.networkService.set(networkService)
                }
            }
        }
    }
