package ru.kode.android.build.publish.plugin.telegram.task.distribution

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
import ru.kode.android.build.publish.plugin.telegram.config.DestinationBot
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
            option = "distributionFile",
            description = "Distribution artifact file (absolute path is expected)",
        )
        abstract val distributionFile: RegularFileProperty

        @get:Input
        @get:Option(option = "destinationBots", description = "Bots which be used to distribute")
        abstract val destinationBots: SetProperty<DestinationBot>

        @TaskAction
        fun upload() {
            val workQueue: WorkQueue = workerExecutor.noIsolation()
            workQueue.submit(TelegramUploadWork::class.java) { parameters ->
                parameters.distributionFile.set(distributionFile)
                parameters.networkService.set(networkService)
                parameters.destinationBots.set(destinationBots)
            }
        }
    }
