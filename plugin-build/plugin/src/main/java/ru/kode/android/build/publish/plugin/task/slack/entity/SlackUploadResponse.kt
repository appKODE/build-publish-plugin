package ru.kode.android.build.publish.plugin.task.slack.entity

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Suppress("ConstructorParameterNaming") // network model
internal data class SlackUploadResponse(
    val ok: Boolean,
    val error: String?
)
