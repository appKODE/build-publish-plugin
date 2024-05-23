package ru.kode.android.build.publish.plugin.task.clickup.entity

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Suppress("ConstructorParameterNaming") // network model
data class AddFieldToTaskRequest(
    val value: String,
)
