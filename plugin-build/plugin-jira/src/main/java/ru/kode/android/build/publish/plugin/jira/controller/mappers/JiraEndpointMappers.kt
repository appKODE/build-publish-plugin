package ru.kode.android.build.publish.plugin.jira.controller.mappers

import kotlinx.serialization.json.Json
import org.gradle.api.GradleException
import ru.kode.android.build.publish.plugin.jira.config.JiraEndpointConfig
import ru.kode.android.build.publish.plugin.jira.controller.entity.JiraEndpoint
import ru.kode.android.build.publish.plugin.jira.messages.jiraEndpointEncodeToJsonFailedMessage
import ru.kode.android.build.publish.plugin.jira.messages.jiraEndpointJsonParsingFailedMessage

@Suppress("TooGenericExceptionCaught")
internal fun JiraEndpoint.toJson(): String {
    return try {
        Json.encodeToString(this)
    } catch (e: Exception) {
        throw GradleException(jiraEndpointEncodeToJsonFailedMessage(), e)
    }
}

@Suppress("TooGenericExceptionCaught")
internal fun jiraEndpointFromJson(json: String): JiraEndpoint {
    return try {
        Json.decodeFromString(json)
    } catch (e: Exception) {
        throw GradleException(jiraEndpointJsonParsingFailedMessage(json), e)
    }
}

internal fun JiraEndpointConfig.mapToEntity(): JiraEndpoint {
    val username = credentials.username.orNull
    val password = credentials.password.orNull
    val endpointName = name

    return JiraEndpoint(
        name = endpointName,
        baseUrl = baseUrl.get(),
        basicAuth =
            if (username != null && password != null) {
                JiraEndpoint.BasicAuth(
                    username = username,
                    password = password,
                )
            } else {
                null
            },
        projectKeys = projectKeys.get().toList(),
    )
}
