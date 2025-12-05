package ru.kode.android.build.publish.plugin.telegram.controller.mappers

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.gradle.api.GradleException
import ru.kode.android.build.publish.plugin.telegram.config.DestinationTelegramBotConfig
import ru.kode.android.build.publish.plugin.telegram.controller.entity.DestinationTelegramBot
import kotlin.collections.toList

internal fun List<DestinationTelegramBot>.toJson(): String {
    return try {
        Json.encodeToString(this)
    } catch (e: Exception) {
        throw GradleException("Failed to serialize DestinationBot list to JSON", e)
    }
}

@Suppress("ThrowsCount")
internal fun destinationTelegramBotsFromJson(json: String): List<DestinationTelegramBot> {
    return try {
        Json.decodeFromString(json)
    } catch (e: Exception) {
        throw GradleException("JSON parsing failed for: $json", e)
    }
}

internal fun Set<DestinationTelegramBotConfig>.mapToEntity(): List<DestinationTelegramBot> {
    return this.map {
        DestinationTelegramBot(
            botName = it.botName.get(),
            chatNames = it.chatNames.get().toList(),
        )
    }
}