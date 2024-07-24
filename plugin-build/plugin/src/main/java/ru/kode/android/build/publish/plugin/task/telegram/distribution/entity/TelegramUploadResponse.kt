package ru.kode.android.build.publish.plugin.task.telegram.distribution.entity

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Suppress("ConstructorParameterNaming") // network model
internal data class TelegramUploadResponse(
    val ok: Boolean,
    val error_code: String?,
)
