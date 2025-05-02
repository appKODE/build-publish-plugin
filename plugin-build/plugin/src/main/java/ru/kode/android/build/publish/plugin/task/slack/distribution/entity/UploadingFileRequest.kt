package ru.kode.android.build.publish.plugin.task.slack.distribution.entity

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UploadingFileRequest(
    val id: String,
    val title: String,
)
