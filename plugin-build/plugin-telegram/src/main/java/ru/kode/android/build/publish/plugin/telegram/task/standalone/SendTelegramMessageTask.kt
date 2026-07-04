package ru.kode.android.build.publish.plugin.telegram.task.standalone

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import ru.kode.android.build.publish.plugin.core.task.StandaloneServiceTask
import ru.kode.android.build.publish.plugin.telegram.service.TelegramService
import ru.kode.android.build.publish.plugin.telegram.task.standalone.work.SendTelegramMessageWork
import javax.inject.Inject

@DisableCachingByDefault
abstract class SendTelegramMessageTask
    @Inject
    constructor(
        workerExecutor: WorkerExecutor,
    ) : StandaloneServiceTask<TelegramService>(workerExecutor) {
        init {
            description = "Sends a message to Telegram"
        }

        @get:Input
        @get:Option(option = "message", description = "Message text to send")
        abstract val message: Property<String>

        @get:Input
        @get:Option(option = "botName", description = "Name of the bot to use for sending")
        abstract val botName: Property<String>

        @get:Input
        @get:Option(option = "chatName", description = "Name of the chat to send the message to")
        abstract val chatName: Property<String>

        @TaskAction
        fun sendMessage() {
            val workQueue = workerExecutor.noIsolation()
            workQueue.submit(SendTelegramMessageWork::class.java) { params ->
                params.message.set(message)
                params.botName.set(botName)
                params.chatName.set(chatName)
                params.service.set(service)
                params.loggerService.set(loggerService)
            }
        }
    }
