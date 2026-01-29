package ru.kode.android.build.publish.plugin.jira.controller.entity

@kotlinx.serialization.Serializable
data class DestinationJiraEndpoint(
    val endpointName: String,
    val projectKeys: List<String>,
)
