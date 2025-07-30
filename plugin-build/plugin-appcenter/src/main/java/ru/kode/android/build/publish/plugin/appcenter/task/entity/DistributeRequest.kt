package ru.kode.android.build.publish.plugin.appcenter.task.entity

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Suppress("ConstructorParameterNaming") // network model
data class DistributeRequest(
    val destinations: List<Destination>,
    val release_notes: String,
    val upload_status: String = "uploadFinished",
    val notify_testers: Boolean = true,
) {
    @JsonClass(generateAdapter = true)
    data class Destination(val name: String)
}
