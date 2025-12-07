package ru.kode.android.build.publish.plugin.jira.network.entity

import kotlinx.serialization.Serializable

@Serializable
internal data class CreateVersionRequest(
    val name: String,
    val projectId: Long,
)