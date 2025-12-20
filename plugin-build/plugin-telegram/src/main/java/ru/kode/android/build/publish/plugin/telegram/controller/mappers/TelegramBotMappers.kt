package ru.kode.android.build.publish.plugin.telegram.controller.mappers

import kotlinx.serialization.json.Json
import org.gradle.api.GradleException
import ru.kode.android.build.publish.plugin.telegram.config.TelegramBotConfig
import ru.kode.android.build.publish.plugin.telegram.controller.entity.TelegramBot
import ru.kode.android.build.publish.plugin.telegram.messages.botWithoutChatMessage
import ru.kode.android.build.publish.plugin.telegram.messages.telegramBotFailedEncodeToJsonMessage
import ru.kode.android.build.publish.plugin.telegram.messages.telegramBotJsonParsingFailedMessage

@Suppress("TooGenericExceptionCaught")
internal fun TelegramBot.toJson(): String {
    return try {
        Json.encodeToString(this)
    } catch (e: Exception) {
        throw GradleException(telegramBotFailedEncodeToJsonMessage(), e)
    }
}

@Suppress("TooGenericExceptionCaught")
internal fun telegramBotFromJson(json: String): TelegramBot {
    return try {
        Json.decodeFromString(json)
    } catch (e: Exception) {
        throw GradleException(telegramBotJsonParsingFailedMessage(json), e)
    }
}

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
