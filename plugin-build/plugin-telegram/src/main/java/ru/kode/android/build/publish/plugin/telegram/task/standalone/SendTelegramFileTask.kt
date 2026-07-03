package ru.kode.android.build.publish.plugin.telegram.task.standalone

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import ru.kode.android.build.publish.plugin.core.task.StandaloneServiceTask
import ru.kode.android.build.publish.plugin.telegram.service.TelegramService
import ru.kode.android.build.publish.plugin.telegram.task.standalone.work.SendTelegramFileWork
import javax.inject.Inject

@DisableCachingByDefault
abstract class SendTelegramFileTask
    @Inject
    constructor(
        workerExecutor: WorkerExecutor,
    ) : StandaloneServiceTask<TelegramService>(workerExecutor) {
        init {
            description = "Uploads a file to Telegram"
        }

        @get:InputFile
        @get:Option(option = "file", description = "File to upload (absolute path)")
        @get:PathSensitive(PathSensitivity.RELATIVE)
        abstract val file: RegularFileProperty

        @get:Input
        @get:Option(option = "botName", description = "Name of the bot to use for uploading")
        abstract val botName: Property<String>

        @get:Input
        @get:Option(option = "chatName", description = "Name of the chat to upload the file to")
        abstract val chatName: Property<String>

        @TaskAction
        fun uploadFile() {
            val workQueue = workerExecutor.noIsolation()
            workQueue.submit(SendTelegramFileWork::class.java) { params ->
                params.file.set(file)
                params.botName.set(botName)
                params.chatName.set(chatName)
                params.service.set(service)
                params.loggerService.set(loggerService)
            }
        }
    }
