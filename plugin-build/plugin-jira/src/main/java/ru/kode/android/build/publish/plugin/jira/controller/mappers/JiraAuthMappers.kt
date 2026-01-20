package ru.kode.android.build.publish.plugin.jira.controller.mappers

import kotlinx.serialization.json.Json
import org.gradle.api.GradleException
import ru.kode.android.build.publish.plugin.jira.config.JiraAuthItemConfig
import ru.kode.android.build.publish.plugin.jira.controller.entity.JiraAuth
import ru.kode.android.build.publish.plugin.jira.messages.jiraAuthEncodeToJsonFailedMessage
import ru.kode.android.build.publish.plugin.jira.messages.jiraAuthJsonParsingFailedMessage

@Suppress("TooGenericExceptionCaught")
internal fun JiraAuth.toJson(): String {
    return try {
        Json.encodeToString(this)
    } catch (e: Exception) {
        throw GradleException(jiraAuthEncodeToJsonFailedMessage(), e)
    }
}

@Suppress("TooGenericExceptionCaught")
internal fun jiraAuthFromJson(json: String): JiraAuth {
    return try {
        Json.decodeFromString(json)
    } catch (e: Exception) {
        throw GradleException(jiraAuthJsonParsingFailedMessage(json), e)
    }
}

internal fun JiraAuthItemConfig.mapToEntity(): JiraAuth {
    val username = credentials.username.orNull
    val password = credentials.password.orNull
    val authName = name

    return JiraAuth(
        name = authName,
        baseUrl = baseUrl.get(),
        basicAuth =
            if (username != null && password != null) {
                JiraAuth.BasicAuth(
                    username = username,
                    password = password,
                )
            } else {
                null
            },
        projectKeys = projectKeys.get().toList(),
    )
}
