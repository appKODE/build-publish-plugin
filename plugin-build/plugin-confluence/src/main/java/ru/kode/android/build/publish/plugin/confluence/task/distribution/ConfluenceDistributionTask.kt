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
import ru.kode.android.build.publish.plugin.confluence.service.ConfluenceNetworkService
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
            option = "buildVariantOutputFile",
            description = "Artifact output file (absolute path is expected)",
        )
        abstract val buildVariantOutputFile: RegularFileProperty

        @get:Input
        @get:Option(
            option = "pageId",
            description = "Id of the Confluence page where file should be uploaded",
        )
        abstract val pageId: Property<String>

        @TaskAction
        fun upload() {
            val outputFile = buildVariantOutputFile.asFile.get()
            val pageId = pageId.get()
            val fileName = outputFile.name
            val workQueue: WorkQueue = workerExecutor.noIsolation()
            workQueue.submit(ConfluenceUploadWork::class.java) { parameters ->
                parameters.outputFile.set(outputFile)
                parameters.pageId.set(pageId)
                parameters.fileName.set(fileName)
                parameters.networkService.set(networkService)
            }
        }
    }
