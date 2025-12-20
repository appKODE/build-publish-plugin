package ru.kode.android.build.publish.plugin.jira.network.entity

import kotlinx.serialization.Serializable

@Serializable
internal data class GetProjectResponse(
    val id: Long,
    val key: String,
    val name: String,
)
