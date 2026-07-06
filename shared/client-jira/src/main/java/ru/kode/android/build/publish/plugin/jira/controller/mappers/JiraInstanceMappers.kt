package ru.kode.android.build.publish.plugin.jira.controller.mappers

import kotlinx.serialization.json.Json
import ru.kode.android.build.publish.plugin.jira.controller.entity.JiraInstanceEntity

/** Encodes a [JiraInstanceEntity] to the JSON baked into the Jira build-service parameters. */
fun JiraInstanceEntity.toJson(): String = Json.encodeToString(this)

/** Decodes a [JiraInstanceEntity] from the JSON baked into the Jira build-service parameters. */
fun jiraInstanceFromJson(json: String): JiraInstanceEntity = Json.decodeFromString(json)
