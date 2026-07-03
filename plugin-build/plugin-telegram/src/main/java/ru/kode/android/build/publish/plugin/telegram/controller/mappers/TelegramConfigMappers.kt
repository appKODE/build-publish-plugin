package ru.kode.android.build.publish.plugin.telegram.controller.mappers

import org.gradle.api.GradleException
import ru.kode.android.build.publish.plugin.telegram.config.DestinationTelegramBotConfig
import ru.kode.android.build.publish.plugin.telegram.config.TelegramBotConfig
import ru.kode.android.build.publish.plugin.telegram.controller.entity.DestinationTelegramBot
import ru.kode.android.build.publish.plugin.telegram.controller.entity.TelegramBot
import ru.kode.android.build.publish.plugin.telegram.messages.botWithoutChatMessage

internal fun TelegramBotConfig.mapToEntity(): TelegramBot {
    val username = botServerAuth.username.orNull
    val password = botServerAuth.password.orNull
    val botName = name
    return TelegramBot(
        name = botName,
        id = botId.get(),
        serverBaseUrl = botServerBaseUrl.orNull,
        basicAuth =
            if (username != null && password != null) {
                TelegramBot.BasicAuth(
                    username = username,
                    password = password,
                )
            } else {
                null
            },
        chats =
            chats.map {
                val id =
                    it.chatId.orNull
                        ?: throw GradleException(botWithoutChatMessage(botName))
                TelegramBot.Chat(
                    name = it.name,
                    id = id,
                    topicId = it.topicId.orNull,
                )
            },
    )
}

@Suppress("TooGenericExceptionCaught")
internal fun Set<DestinationTelegramBotConfig>.mapToEntity(): List<DestinationTelegramBot> {
    return this.map {
        DestinationTelegramBot(
            botName = it.botName.get(),
            chatNames = it.chatNames.get().toList(),
        )
    }
}
