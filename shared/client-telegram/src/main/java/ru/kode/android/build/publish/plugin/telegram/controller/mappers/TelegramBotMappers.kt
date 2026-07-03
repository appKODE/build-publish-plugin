package ru.kode.android.build.publish.plugin.telegram.controller.mappers

import kotlinx.serialization.json.Json
import ru.kode.android.build.publish.plugin.telegram.controller.entity.TelegramBot
import ru.kode.android.build.publish.plugin.telegram.messages.telegramBotFailedEncodeToJsonMessage
import ru.kode.android.build.publish.plugin.telegram.messages.telegramBotJsonParsingFailedMessage

@Suppress("TooGenericExceptionCaught")
fun TelegramBot.toJson(): String {
    return try {
        Json.encodeToString(this)
    } catch (e: Exception) {
        throw IllegalStateException(telegramBotFailedEncodeToJsonMessage(), e)
    }
}

@Suppress("TooGenericExceptionCaught")
fun telegramBotFromJson(json: String): TelegramBot {
    return try {
        Json.decodeFromString(json)
    } catch (e: Exception) {
        throw IllegalStateException(telegramBotJsonParsingFailedMessage(json), e)
    }
}
