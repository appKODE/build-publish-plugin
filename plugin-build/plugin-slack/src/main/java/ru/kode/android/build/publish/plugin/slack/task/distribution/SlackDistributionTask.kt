package ru.kode.android.build.publish.plugin.slack.task.distribution

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import ru.kode.android.build.publish.plugin.core.git.mapper.fromJson
import ru.kode.android.build.publish.plugin.slack.service.SlackService
import ru.kode.android.build.publish.plugin.slack.task.distribution.work.SlackUploadWork
import javax.inject.Inject

/**
 * A Gradle task that handles uploading build artifacts to Slack for distribution.
 *
 * This task is responsible for:
 * - Uploading APK or bundle files to Slack
 * - Sharing the uploaded files in specified channels
 * - Tracking build information using build tags
 */
abstract class SlackDistributionTask
    @Inject
    constructor(
        private val workerExecutor: WorkerExecutor,
    ) : DefaultTask() {
        init {
            description = "Task to send apk to Slack"
            group = BasePlugin.BUILD_GROUP
        }

        /**
         * The network service property provides access to the Slack upload service used for
         * uploading distribution files to Slack.
         *
         * This property is internal and should not be directly accessed by other plugins.
         * It is injected by Gradle when the task is created and configured.
         */
        @get:Internal
        abstract val service: Property<SlackService>

        /**
         * The build tag file property contains metadata about the current build.
         */
        @get:InputFile
        @get:Option(option = "buildTagFile", description = "Json contains info about tag build")
        abstract val buildTagFile: RegularFileProperty

        /**
         * The base output file name property specifies the name prefix for the uploaded file.
         */
        @get:Input
        @get:Option(
            option = "baseOutputFileName",
            description = "Application bundle name for changelog",
        )
        abstract val baseOutputFileName: Property<String>

        /**
         * The distribution file property points to the build artifact that will be uploaded to Slack.
         *
         * This should be the absolute path to either:
         * - A compiled APK file (for debug/release builds)
         * - An Android App Bundle (AAB) file (for production releases)
         *
         * The file must exist at the specified path before this task is executed.
         */
        @get:InputFile
        @get:Option(
            option = "distributionFile",
            description = "Artifact output file (absolute path is expected)",
        )
        abstract val distributionFile: RegularFileProperty

        /**
         * The destination channels property specifies the Slack channels where the build artifact
         * will be shared.
         *
         * Channels can be specified in one of the following formats:
         * - Public channels: "#channel-name"
         * - Private groups: "group-name"
         * - Direct messages: "@username"
         *
         * The bot must be a member of all specified channels before uploading files.
         */
        @get:Option(
            option = "channels",
            description = "Public channels where file will be uploaded",
        )
        @get:Input
        abstract val destinationChannels: SetProperty<String>

        /**
         * Executes the file upload to Slack.
         *
         * This method:
         * 1. Reads the build tag information
         * 2. Submits the upload work to a worker thread
         * 3. Configures the work with all necessary parameters
         *
         * The actual upload is performed asynchronously by a Gradle worker.
         */
        @TaskAction
        fun upload() {
            val currentBuildTag = fromJson(buildTagFile.asFile.get())
            val workQueue: WorkQueue = workerExecutor.noIsolation()
            workQueue.submit(SlackUploadWork::class.java) { parameters ->
                parameters.distributionFile.set(distributionFile)
                parameters.destinationChannels.set(destinationChannels)
                parameters.buildName.set(currentBuildTag.name)
                parameters.baseOutputFileName.set(baseOutputFileName)
                parameters.service.set(service)
            }
        }
    }
