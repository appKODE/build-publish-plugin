package ru.kode.android.build.publish.plugin.telegram.task.standalone.work

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import ru.kode.android.build.publish.plugin.core.task.ServiceWorkParameters
import ru.kode.android.build.publish.plugin.telegram.controller.entity.DestinationTelegramBot
import ru.kode.android.build.publish.plugin.telegram.messages.uploadingTelegramFileMessage
import ru.kode.android.build.publish.plugin.telegram.service.TelegramService
import javax.inject.Inject

internal interface SendTelegramFileParameters : ServiceWorkParameters {
    val file: RegularFileProperty
    val botName: Property<String>
    val chatName: Property<String>
    val service: Property<TelegramService>
}

internal abstract class SendTelegramFileWork
    @Inject
    constructor() : WorkAction<SendTelegramFileParameters> {
        override fun execute() {
            parameters.loggerService.get().info(uploadingTelegramFileMessage())
            val destinationBot =
                DestinationTelegramBot(
                    botName = parameters.botName.get(),
                    chatNames = listOf(parameters.chatName.get()),
                )
            parameters.service.get().upload(
                file = parameters.file.asFile.get(),
                destinationBots = listOf(destinationBot),
            )
        }
    }
