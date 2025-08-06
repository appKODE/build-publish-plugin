package ru.kode.android.build.publish.plugin.slack.task.distribution.entity

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class UploadingFileRequest(
    val id: String,
    val title: String,
)
