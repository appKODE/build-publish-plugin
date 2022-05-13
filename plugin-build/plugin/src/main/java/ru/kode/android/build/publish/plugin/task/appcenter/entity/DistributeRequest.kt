package ru.kode.android.build.publish.plugin.task.appcenter.entity

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DistributeRequest(
    val destinations: List<Destination>,
    val release_notes: String,
    val upload_status: String = "uploadFinished",
    val notify_testers: Boolean = true,
) {
    @JsonClass(generateAdapter = true)
    data class Destination(val name: String)
}