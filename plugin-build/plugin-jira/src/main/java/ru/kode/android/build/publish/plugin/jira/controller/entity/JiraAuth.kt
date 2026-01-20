package ru.kode.android.build.publish.plugin.jira.controller.entity

@kotlinx.serialization.Serializable
data class JiraAuth(
    val name: String,
    val baseUrl: String,
    val basicAuth: BasicAuth?,
    val projectKeys: List<String>,
) {
    @kotlinx.serialization.Serializable
    data class BasicAuth(
        val username: String,
        val password: String,
    )
}
