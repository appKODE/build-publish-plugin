package ru.kode.android.build.publish.plugin.telegram.controller.mappers

import kotlinx.serialization.json.Json
import org.gradle.api.GradleException
import ru.kode.android.build.publish.plugin.telegram.config.DestinationTelegramBotConfig
import ru.kode.android.build.publish.plugin.telegram.controller.entity.DestinationTelegramBot
import ru.kode.android.build.publish.plugin.telegram.messages.destinationBotsEncodeToJsonFailedMessage
import ru.kode.android.build.publish.plugin.telegram.messages.destinationBotsJsonParsingFailedMessage
import kotlin.collections.toList

@Suppress("TooGenericExceptionCaught")
internal fun List<DestinationTelegramBot>.toJson(): String {
    return try {
        Json.encodeToString(this)
    } catch (e: Exception) {
        throw GradleException(destinationBotsEncodeToJsonFailedMessage(), e)
    }
}

@Suppress("TooGenericExceptionCaught")
internal fun destinationTelegramBotsFromJson(json: String): List<DestinationTelegramBot> {
    return try {
        Json.decodeFromString(json)
    } catch (e: Exception) {
        throw GradleException(destinationBotsJsonParsingFailedMessage(json), e)
    }
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
