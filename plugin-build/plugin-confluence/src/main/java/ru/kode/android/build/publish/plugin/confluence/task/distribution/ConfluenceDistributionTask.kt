package ru.kode.android.build.publish.plugin.confluence.task.distribution

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import ru.kode.android.build.publish.plugin.confluence.service.network.ConfluenceNetworkService
import ru.kode.android.build.publish.plugin.confluence.task.distribution.work.ConfluenceUploadWork
import javax.inject.Inject

abstract class ConfluenceDistributionTask
    @Inject
    constructor(
        private val workerExecutor: WorkerExecutor,
    ) : DefaultTask() {
        init {
            description = "Task to send apk to Confluence"
            group = BasePlugin.BUILD_GROUP
        }

        @get:Internal
        abstract val networkService: Property<ConfluenceNetworkService>

        @get:InputFile
        @get:Option(
            option = "distributionFile",
            description = "Artifact output file (absolute path is expected)",
        )
        abstract val distributionFile: RegularFileProperty

        @get:Input
        @get:Option(
            option = "pageId",
            description = "Id of the Confluence page where file should be uploaded",
        )
        abstract val pageId: Property<String>

        @TaskAction
        fun upload() {
            val pageId = pageId.get()
            val workQueue: WorkQueue = workerExecutor.noIsolation()
            workQueue.submit(ConfluenceUploadWork::class.java) { parameters ->
                parameters.outputFile.set(distributionFile)
                parameters.pageId.set(pageId)
                parameters.networkService.set(networkService)
            }
        }
    }
