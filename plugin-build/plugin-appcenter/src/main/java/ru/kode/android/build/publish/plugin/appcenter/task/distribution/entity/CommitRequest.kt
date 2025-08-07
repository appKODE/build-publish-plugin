package ru.kode.android.build.publish.plugin.appcenter.task.distribution.entity

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Suppress("ConstructorParameterNaming") // network model
internal data class CommitRequest(
    val id: String,
    val upload_status: String = "uploadFinished",
)
