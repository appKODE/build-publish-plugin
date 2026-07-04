package ru.kode.android.build.publish.plugin.telegram.task.standalone.work

import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import ru.kode.android.build.publish.plugin.core.task.ServiceWorkParameters
import ru.kode.android.build.publish.plugin.telegram.controller.entity.DestinationTelegramBot
import ru.kode.android.build.publish.plugin.telegram.messages.sendingTelegramMessageMessage
import ru.kode.android.build.publish.plugin.telegram.service.TelegramService
import javax.inject.Inject

internal interface SendTelegramMessageParameters : ServiceWorkParameters {
    val message: Property<String>
    val botName: Property<String>
    val chatName: Property<String>
    val service: Property<TelegramService>
}

internal abstract class SendTelegramMessageWork
    @Inject
    constructor() : WorkAction<SendTelegramMessageParameters> {
        override fun execute() {
            parameters.loggerService.get().info(sendingTelegramMessageMessage())
            val destinationBot =
                DestinationTelegramBot(
                    botName = parameters.botName.get(),
                    chatNames = listOf(parameters.chatName.get()),
                )
            parameters.service.get().send(
                changelog = parameters.message.get(),
                header = "",
                userMentions = emptyList(),
                issueSources = emptyList(),
                destinationBots = listOf(destinationBot),
            )
        }
    }
