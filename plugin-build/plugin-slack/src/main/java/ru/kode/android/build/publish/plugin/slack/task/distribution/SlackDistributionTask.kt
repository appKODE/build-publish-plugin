package ru.kode.android.build.publish.plugin.slack.task.distribution

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import ru.kode.android.build.publish.plugin.core.mapper.fromJson
import ru.kode.android.build.publish.plugin.slack.task.distribution.work.SlackUploadWork
import javax.inject.Inject

abstract class SlackDistributionTask
    @Inject
    constructor(
        private val workerExecutor: WorkerExecutor,
    ) : DefaultTask() {
        init {
            description = "Task to send apk to Slack"
            group = BasePlugin.BUILD_GROUP
        }

        @get:InputFile
        @get:Option(option = "tagBuildFile", description = "Json contains info about tag build")
        abstract val tagBuildFile: RegularFileProperty

        @get:Input
        @get:Option(
            option = "baseOutputFileName",
            description = "Application bundle name for changelog",
        )
        abstract val baseOutputFileName: Property<String>

        @get:InputFile
        @get:Option(
            option = "buildVariantOutputFile",
            description = "Artifact output file (absolute path is expected)",
        )
        abstract val buildVariantOutputFile: RegularFileProperty

        @get:InputFile
        @get:Option(
            option = "channels",
            description = " Api token file to upload files in slack",
        )
        abstract val apiTokenFile: RegularFileProperty

        @get:Option(
            option = "channels",
            description = "Public channels where file will be uploaded",
        )
        @get:Input
        abstract val channels: SetProperty<String>

        @TaskAction
        fun upload() {
            val outputFile = buildVariantOutputFile.asFile.get()
            val apiToken = apiTokenFile.asFile.get().readText()
            val channels = channels.get()
            val currentBuildTag = fromJson(tagBuildFile.asFile.get())
            val workQueue: WorkQueue = workerExecutor.noIsolation()
            workQueue.submit(SlackUploadWork::class.java) { parameters ->
                parameters.apiToken.set(apiToken)
                parameters.outputFile.set(outputFile)
                parameters.channels.set(channels)
                parameters.buildName.set(currentBuildTag.name)
                parameters.baseOutputFileName.set(baseOutputFileName)
            }
        }
    }
