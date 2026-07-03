package ru.kode.android.build.publish.plugin.sender.task.telegram.work

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.sender.task.telegram.buildSenderBot
import ru.kode.android.build.publish.plugin.telegram.controller.TelegramControllerFactory
import javax.inject.Inject

internal interface SendTelegramFileParameters : WorkParameters {
    val botId: Property<String>
    val chatId: Property<String>
    val topicId: Property<String>
    val serverBaseUrl: Property<String>
    val file: RegularFileProperty
}

internal abstract class SendTelegramFileWork
    @Inject
    constructor() : WorkAction<SendTelegramFileParameters> {
        override fun execute() {
            val bot =
                buildSenderBot(
                    botId = parameters.botId.get(),
                    chatId = parameters.chatId.get(),
                    topicId = parameters.topicId.get(),
                    serverBaseUrl = parameters.serverBaseUrl.get(),
                )
            TelegramControllerFactory.build().upload(
                file = parameters.file.asFile.get(),
                bots = listOf(bot),
            )
        }
    }
