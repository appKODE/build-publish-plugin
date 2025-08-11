package ru.kode.android.build.publish.plugin.task.telegram.distribution

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import ru.kode.android.build.publish.plugin.task.telegram.distribution.work.TelegramUploadWork
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

        @get:InputFile
        @get:Option(
            option = "buildVariantOutputFile",
            description = "Artifact output file (absolute path is expected)",
        )
        abstract val buildVariantOutputFile: RegularFileProperty

        @get:Input
        @get:Option(option = "botId", description = "Bot id where webhook posted")
        abstract val botId: Property<String>

        @get:Input
        @get:Option(option = "botBaseUrl", description = "Bot server base url")
        abstract val botBaseUrl: Property<String>

        @get:Input
        @get:Option(option = "botBaseUrl", description = "Bot server auth username")
        abstract val botAuthUsername: Property<String>

        @get:Input
        @get:Option(option = "botAuthPassword", description = "Bot server auth password")
        abstract val botAuthPassword: Property<String>

        @get:Input
        @get:Option(option = "chatId", description = "Chat id where webhook posted")
        abstract val chatId: Property<String>

        @get:Input
        @get:Optional
        @get:Option(option = "topicId", description = "Unique identifier for the target message thread")
        abstract val topicId: Property<String>

        @TaskAction
        fun upload() {
            val outputFile = buildVariantOutputFile.asFile.get()
            val botId = botId.get()
            val chatId = chatId.get()
            val topicId = topicId.orNull
            val workQueue: WorkQueue = workerExecutor.noIsolation()
            workQueue.submit(TelegramUploadWork::class.java) { parameters ->
                parameters.outputFile.set(outputFile)
                parameters.botId.set(botId)
                parameters.chatId.set(chatId)
                parameters.topicId.set(topicId)
                parameters.botBaseUrl.set(botBaseUrl)
                parameters.botAuthUsername.set(botAuthUsername)
                parameters.botAuthPassword.set(botAuthPassword)
            }
        }
    }
