package ru.kode.android.build.publish.plugin.telegram.controller

import ru.kode.android.build.publish.plugin.telegram.controller.entity.ChatSpecificTelegramBot
import ru.kode.android.build.publish.plugin.telegram.controller.entity.TelegramLastMessage
import java.io.File

interface TelegramController {
    fun send(message: TelegramMessage)

    fun upload(
        file: File,
        bots: List<ChatSpecificTelegramBot>,
    )

    fun getLastMessage(
        botId: String,
        chatName: String,
        topicName: String?,
    ): TelegramLastMessage?
}
