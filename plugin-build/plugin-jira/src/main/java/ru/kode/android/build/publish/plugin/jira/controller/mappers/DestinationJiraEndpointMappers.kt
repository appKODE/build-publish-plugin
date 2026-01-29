package ru.kode.android.build.publish.plugin.jira.controller.mappers

import kotlinx.serialization.json.Json
import org.gradle.api.GradleException
import ru.kode.android.build.publish.plugin.jira.config.DestinationJiraEndpointConfig
import ru.kode.android.build.publish.plugin.jira.controller.entity.DestinationJiraEndpoint
import ru.kode.android.build.publish.plugin.jira.messages.destinationEndpointEncodeToJsonFailedMessage
import ru.kode.android.build.publish.plugin.jira.messages.destinationEndpointJsonParsingFailedMessage

@Suppress("TooGenericExceptionCaught")
internal fun List<DestinationJiraEndpoint>.toJson(): String {
    return try {
        Json.encodeToString(this)
    } catch (e: Exception) {
        throw GradleException(destinationEndpointEncodeToJsonFailedMessage(), e)
    }
}

@Suppress("TooGenericExceptionCaught")
internal fun destinationJiraEndpointsFromJson(json: String): List<DestinationJiraEndpoint> {
    return try {
        Json.decodeFromString(json)
    } catch (e: Exception) {
        throw GradleException(destinationEndpointJsonParsingFailedMessage(json), e)
    }
}

@Suppress("TooGenericExceptionCaught")
internal fun Set<DestinationJiraEndpointConfig>.mapToEntity(): List<DestinationJiraEndpoint> {
    return this.map {
        DestinationJiraEndpoint(
            endpointName = it.endpointName.get(),
            projectKeys = it.projectKeys.get().toList(),
        )
    }
}
