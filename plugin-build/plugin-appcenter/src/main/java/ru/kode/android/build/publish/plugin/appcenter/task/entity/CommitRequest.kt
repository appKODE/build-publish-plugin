package ru.kode.android.build.publish.plugin.appcenter.task.entity

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Suppress("ConstructorParameterNaming") // network model
data class CommitRequest(
    val id: String,
    val upload_status: String = "uploadFinished",
)
