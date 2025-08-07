package ru.kode.android.build.publish.plugin.clickup.task.automation

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
import ru.kode.android.build.publish.plugin.clickup.service.network.ClickUpNetworkService
import ru.kode.android.build.publish.plugin.clickup.task.automation.work.AddFixVersionWork
import ru.kode.android.build.publish.plugin.clickup.task.automation.work.AddTagToTaskWork
import ru.kode.android.build.publish.plugin.core.enity.Tag
import ru.kode.android.build.publish.plugin.core.git.mapper.fromJson
import javax.inject.Inject

abstract class ClickUpAutomationTask
    @Inject
    constructor(
        private val workerExecutor: WorkerExecutor,
    ) : DefaultTask() {
        init {
            description = "Task to automate ClickUp statuses"
            group = BasePlugin.BUILD_GROUP
        }

        @get:Internal
        abstract val networkService: Property<ClickUpNetworkService>

        @get:InputFile
        @get:Option(option = "tagBuildFile", description = "Json contains info about tag build")
        abstract val tagBuildFile: RegularFileProperty

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
            option = "fixVersionPattern",
            description = "How fix version should be formatted",
        )
        @get:Optional
        abstract val fixVersionPattern: Property<String>

        @get:Input
        @get:Option(
            option = "fixVersionFieldId",
            description = "Field id for fix version in ClickUp",
        )
        @get:Optional
        abstract val fixVersionFieldId: Property<String>

        @get:Input
        @get:Option(
            option = "taskTag",
            description = "What tag should be used for tasks",
        )
        @get:Optional
        abstract val taskTag: Property<String>

        @TaskAction
        fun upload() {
            val currentBuildTag = fromJson(tagBuildFile.asFile.get())
            val changelog = changelogFile.asFile.get().readText()
            val issues =
                Regex(issueNumberPattern.get())
                    .findAll(changelog)
                    .mapTo(mutableSetOf()) { it.groupValues[0] }
            if (issues.isEmpty()) {
                logger.info("issues not found in the changelog, nothing will change")
            } else {
                val workQueue: WorkQueue = workerExecutor.noIsolation()
                workQueue.submitUpdateVersionIfPresent(currentBuildTag, issues)
                workQueue.submitSetTagIfPresent(issues)
            }
        }

        private fun WorkQueue.submitUpdateVersionIfPresent(
            currentBuildTag: Tag.Build,
            issues: Set<String>,
        ) {
            if (fixVersionPattern.isPresent && fixVersionFieldId.isPresent) {
                val version =
                    fixVersionPattern.get()
                        .format(
                            currentBuildTag.buildVersion,
                            currentBuildTag.buildNumber,
                            currentBuildTag.buildVariant,
                        )
                val fieldId = fixVersionFieldId.get()
                submit(AddFixVersionWork::class.java) { parameters ->
                    parameters.issues.set(issues)
                    parameters.version.set(version)
                    parameters.fieldId.set(fieldId)
                    parameters.networkService.set(networkService)
                }
            }
        }

        private fun WorkQueue.submitSetTagIfPresent(issues: Set<String>) {
            if (taskTag.isPresent) {
                val tagName = taskTag.get()
                submit(AddTagToTaskWork::class.java) { parameters ->
                    parameters.issues.set(issues)
                    parameters.tagName.set(tagName)
                    parameters.networkService.set(networkService)
                }
            }
        }
    }
