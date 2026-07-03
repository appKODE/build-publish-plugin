package ru.kode.android.build.publish.plugin.sender.task.telegram

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import ru.kode.android.build.publish.plugin.sender.task.base.BaseTelegramSenderTask
import ru.kode.android.build.publish.plugin.sender.task.telegram.work.SendTelegramFileWork
import javax.inject.Inject

@DisableCachingByDefault
abstract class SendTelegramFileTask
    @Inject
    constructor(
        workerExecutor: WorkerExecutor,
    ) : BaseTelegramSenderTask(workerExecutor) {
        init {
            description = "Uploads a file to Telegram"
        }

        @get:InputFile
        @get:PathSensitive(PathSensitivity.RELATIVE)
        @get:Option(option = "file", description = "File to upload")
        abstract val file: RegularFileProperty

        @TaskAction
        fun uploadFile() {
            workerExecutor.noIsolation().submit(SendTelegramFileWork::class.java) { params ->
                params.botId.set(botId)
                params.chatId.set(chatId)
                params.topicId.set(topicId.orElse(""))
                params.serverBaseUrl.set(serverBaseUrl.orElse(""))
                params.file.set(file)
            }
        }
    }
