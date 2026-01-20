package ru.kode.android.build.publish.plugin.jira.controller.mappers

import kotlinx.serialization.json.Json
import org.gradle.api.GradleException
import ru.kode.android.build.publish.plugin.jira.config.DestinationJiraAuthConfig
import ru.kode.android.build.publish.plugin.jira.controller.entity.DestinationJiraAuth
import ru.kode.android.build.publish.plugin.jira.messages.destinationAuthEncodeToJsonFailedMessage
import ru.kode.android.build.publish.plugin.jira.messages.destinationAuthJsonParsingFailedMessage

@Suppress("TooGenericExceptionCaught")
internal fun List<DestinationJiraAuth>.toJson(): String {
    return try {
        Json.encodeToString(this)
    } catch (e: Exception) {
        throw GradleException(destinationAuthEncodeToJsonFailedMessage(), e)
    }
}

@Suppress("TooGenericExceptionCaught")
internal fun destinationJiraAuthsFromJson(json: String): List<DestinationJiraAuth> {
    return try {
        Json.decodeFromString(json)
    } catch (e: Exception) {
        throw GradleException(destinationAuthJsonParsingFailedMessage(json), e)
    }
}

@Suppress("TooGenericExceptionCaught")
internal fun Set<DestinationJiraAuthConfig>.mapToEntity(): List<DestinationJiraAuth> {
    return this.map {
        DestinationJiraAuth(
            authName = it.authName.get(),
            projectKeys = it.projectKeys.get().toList(),
        )
    }
}
