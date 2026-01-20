package ru.kode.android.build.publish.plugin.jira.controller.entity

@kotlinx.serialization.Serializable
data class DestinationJiraAuth(
    val authName: String,
    val projectKeys: List<String>,
)
