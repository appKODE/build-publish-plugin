package ru.kode.android.build.publish.plugin.jira.controller.entity

import kotlinx.serialization.Serializable

/**
 * A single Jira instance baked into the Jira build-service parameters as JSON: its [name], [baseUrl],
 * credentials, and the [projects] declared on it.
 *
 * Mirrors the Telegram model where a single build service bakes all of its targets (bots + chats) as
 * JSON, so one Jira service per build variant can serve every declared instance.
 */
@Serializable
data class JiraInstanceEntity(
    val name: String,
    val baseUrl: String,
    val username: String,
    val password: String,
    val projects: List<JiraProjectEntity>,
)
