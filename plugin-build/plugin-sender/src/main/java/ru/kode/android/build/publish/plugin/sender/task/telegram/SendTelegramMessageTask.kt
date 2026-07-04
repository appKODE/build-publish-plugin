package ru.kode.android.build.publish.plugin.sender.task.telegram

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import ru.kode.android.build.publish.plugin.sender.task.base.BaseTelegramSenderTask
import ru.kode.android.build.publish.plugin.sender.task.telegram.work.SendTelegramMessageWork
import javax.inject.Inject

@DisableCachingByDefault
abstract class SendTelegramMessageTask
    @Inject
    constructor(
        workerExecutor: WorkerExecutor,
    ) : BaseTelegramSenderTask(workerExecutor) {
        init {
            description = "Sends a message to Telegram"
        }

        @get:Input
        @get:Option(option = "message", description = "Message text to send")
        abstract val message: Property<String>

        @TaskAction
        fun sendMessage() {
            workerExecutor.noIsolation().submit(SendTelegramMessageWork::class.java) { params ->
                params.botId.set(botId)
                params.chatId.set(chatId)
                params.topicId.set(topicId.orElse(""))
                params.serverBaseUrl.set(serverBaseUrl.orElse(""))
                params.message.set(message)
            }
        }
    }
