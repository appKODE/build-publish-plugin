package ru.kode.android.build.publish.plugin.telegram.controller.mappers

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.gradle.api.GradleException
import ru.kode.android.build.publish.plugin.telegram.config.TelegramBotConfig
import ru.kode.android.build.publish.plugin.telegram.controller.entity.TelegramBot

internal fun TelegramBot.toJson(): String {
    return try {
        Json.encodeToString(this)
    } catch (e: Exception) {
        throw GradleException("Failed to serialize TelegramBot to JSON", e)
    }
}

@Suppress("ThrowsCount")
internal fun telegramBotFromJson(json: String): TelegramBot {
    return try {
        Json.decodeFromString(json)
    } catch (e: Exception) {
        throw GradleException("JSON parsing failed for TelegramBot: $json", e)
    }
}

internal fun TelegramBotConfig.mapToEntity(): TelegramBot {
    val username = botServerAuth.username.orNull
    val password = botServerAuth.password.orNull
    return TelegramBot(
        name = name,
        id = botId.get(),
        serverBaseUrl = botServerBaseUrl.orNull,
        basicAuth = if (username != null && password != null) {
            TelegramBot.BasicAuth(
                username = username,
                password = password,
            )
        } else null,
        chats = chats.map {
            TelegramBot.Chat(
                name = it.name,
                id = it.chatId.get(),
                topicId = it.topicId.orNull,
            )
        }
    )
}
