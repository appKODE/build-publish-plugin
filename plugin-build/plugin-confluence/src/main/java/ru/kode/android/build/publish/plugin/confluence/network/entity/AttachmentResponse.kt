package ru.kode.android.build.publish.plugin.confluence.network.entity

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class AttachmentResponse(
    val results: List<Attachment>
) {
    @JsonClass(generateAdapter = true)
    internal data class Attachment(
        val id: String,
        val title: String,
    )
}
