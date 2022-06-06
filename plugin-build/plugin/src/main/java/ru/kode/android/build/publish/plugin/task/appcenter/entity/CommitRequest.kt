package ru.kode.android.build.publish.plugin.task.appcenter.entity

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Suppress("ConstructorParameterNaming") // network model
data class CommitRequest(
    val id: String,
    val upload_status: String = "uploadFinished",
)
