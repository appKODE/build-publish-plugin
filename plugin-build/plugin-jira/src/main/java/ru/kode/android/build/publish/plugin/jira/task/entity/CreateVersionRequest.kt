package ru.kode.android.build.publish.plugin.jira.task.entity

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Suppress("ConstructorParameterNaming") // network model
data class CreateVersionRequest(
    val name: String,
    val projectId: Long,
)
