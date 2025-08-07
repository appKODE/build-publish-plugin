package ru.kode.android.build.publish.plugin.telegram.task.distribution

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import ru.kode.android.build.publish.plugin.telegram.service.network.TelegramNetworkService
import ru.kode.android.build.publish.plugin.telegram.task.distribution.work.TelegramUploadWork
import javax.inject.Inject

abstract class TelegramDistributionTask
    @Inject
    constructor(
        private val workerExecutor: WorkerExecutor,
    ) : DefaultTask() {
        init {
            description = "Task to send apk to Telegram"
            group = BasePlugin.BUILD_GROUP
        }

        @get:Internal
        abstract val networkService: Property<TelegramNetworkService>

        @get:InputFile
        @get:Option(
            option = "buildVariantOutputFile",
            description = "Artifact output file (absolute path is expected)",
        )
        abstract val buildVariantOutputFile: RegularFileProperty

        @TaskAction
        fun upload() {
            val outputFile = buildVariantOutputFile.asFile.get()
            val workQueue: WorkQueue = workerExecutor.noIsolation()
            workQueue.submit(TelegramUploadWork::class.java) { parameters ->
                parameters.outputFile.set(outputFile)
                parameters.networkService.set(networkService)
            }
        }
    }
