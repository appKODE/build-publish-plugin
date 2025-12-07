package ru.kode.android.build.publish.plugin.jira.network.entity

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Suppress("ConstructorParameterNaming") // network model
internal data class JiraFixVersion(
    val id: String,
    val name: String,
    val self: String,
)