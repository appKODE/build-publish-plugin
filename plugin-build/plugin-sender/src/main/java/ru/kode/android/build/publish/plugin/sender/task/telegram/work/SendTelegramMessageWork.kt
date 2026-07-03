package ru.kode.android.build.publish.plugin.sender.task.telegram.work

import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.sender.task.telegram.buildSenderBot
import ru.kode.android.build.publish.plugin.telegram.controller.TelegramControllerFactory
import ru.kode.android.build.publish.plugin.telegram.controller.TelegramMessage
import javax.inject.Inject

internal interface SendTelegramMessageParameters : WorkParameters {
    val botId: Property<String>
    val chatId: Property<String>
    val topicId: Property<String>
    val serverBaseUrl: Property<String>
    val message: Property<String>
}

internal abstract class SendTelegramMessageWork
    @Inject
    constructor() : WorkAction<SendTelegramMessageParameters> {
        override fun execute() {
            val bot =
                buildSenderBot(
                    botId = parameters.botId.get(),
                    chatId = parameters.chatId.get(),
                    topicId = parameters.topicId.get(),
                    serverBaseUrl = parameters.serverBaseUrl.get(),
                )
            TelegramControllerFactory.build().send(
                TelegramMessage(
                    text = parameters.message.get(),
                    bots = listOf(bot),
                ),
            )
        }
    }
