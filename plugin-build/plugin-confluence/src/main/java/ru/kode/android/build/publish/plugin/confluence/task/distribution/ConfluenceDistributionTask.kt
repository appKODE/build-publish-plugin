package ru.kode.android.build.publish.plugin.confluence.task.distribution

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import ru.kode.android.build.publish.plugin.confluence.service.ConfluenceService
import ru.kode.android.build.publish.plugin.confluence.task.distribution.work.ConfluenceUploadWork
import ru.kode.android.build.publish.plugin.core.logger.LoggerService
import javax.inject.Inject

/**
 * A Gradle task that handles uploading distribution files to Confluence.
 *
 * This task is responsible for uploading APK or other distribution files to a specified Confluence page.
 * It uses Gradle's worker API to perform the upload asynchronously.
 */
abstract class ConfluenceDistributionTask
    @Inject
    constructor(
        private val workerExecutor: WorkerExecutor,
    ) : DefaultTask() {
        init {
            description = "Uploads distribution files to a Confluence page"
            group = BasePlugin.BUILD_GROUP
        }

        /**
         * The network service for interacting with the Confluence REST API.
         *
         * This property is injected by the plugin and is used to perform the actual network operations.
         * It is internal and not meant to be accessed or configured by the user.
         */
        @get:Internal
        abstract val service: Property<ConfluenceService>

        /**
         * The logger service used for logging.
         *
         * This is an internal property that's injected when the task is created.
         */
        @get:ServiceReference
        abstract val loggerService: Property<LoggerService>

        /**
         * The distribution file to be uploaded to Confluence.
         *
         * This property is wired by the plugin when the task is registered.
         * It can be overridden via CLI using the task option.
         */
        @get:InputFile
        @get:Option(
            option = "distributionFile",
            description = "Artifact output file (absolute path is expected)",
        )
        abstract val distributionFile: RegularFileProperty

        /**
         * The ID of the Confluence page where the file should be uploaded.
         *
         * This property is wired from the Confluence distribution configuration when the task is registered.
         * It can be overridden via CLI using the task option.
         */
        @get:Input
        @get:Option(
            option = "pageId",
            description = "Id of the Confluence page where file should be uploaded",
        )
        abstract val pageId: Property<String>

        /**
         * Executes the task to upload the distribution file to Confluence.
         *
         * This method retrieves the [pageId] and [distributionFile] properties,
         * initializes a work queue, and submits a [ConfluenceUploadWork] task to the queue.
         * The task is configured with the necessary parameters such as the output file,
         * the Confluence page ID, and the network service.
         */
        @TaskAction
        fun upload() {
            val pageId = pageId.get()
            val workQueue: WorkQueue = workerExecutor.noIsolation()
            workQueue.submit(ConfluenceUploadWork::class.java) { parameters ->
                parameters.outputFile.set(distributionFile)
                parameters.pageId.set(pageId)
                parameters.service.set(service)
                parameters.loggerService.set(loggerService)
            }
        }
    }
