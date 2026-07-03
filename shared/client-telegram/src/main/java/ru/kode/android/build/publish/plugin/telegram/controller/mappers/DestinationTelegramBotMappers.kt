package ru.kode.android.build.publish.plugin.telegram.controller.mappers

import kotlinx.serialization.json.Json
import ru.kode.android.build.publish.plugin.telegram.controller.entity.DestinationTelegramBot
import ru.kode.android.build.publish.plugin.telegram.messages.destinationBotsEncodeToJsonFailedMessage
import ru.kode.android.build.publish.plugin.telegram.messages.destinationBotsJsonParsingFailedMessage

@Suppress("TooGenericExceptionCaught")
fun List<DestinationTelegramBot>.toJson(): String {
    return try {
        Json.encodeToString(this)
    } catch (e: Exception) {
        throw IllegalStateException(destinationBotsEncodeToJsonFailedMessage(), e)
    }
}

@Suppress("TooGenericExceptionCaught")
fun destinationTelegramBotsFromJson(json: String): List<DestinationTelegramBot> {
    return try {
        Json.decodeFromString(json)
    } catch (e: Exception) {
        throw IllegalStateException(destinationBotsJsonParsingFailedMessage(json), e)
    }
}
