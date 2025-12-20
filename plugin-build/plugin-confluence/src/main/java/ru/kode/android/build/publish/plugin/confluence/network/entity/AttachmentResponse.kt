package ru.kode.android.build.publish.plugin.confluence.network.entity

import kotlinx.serialization.Serializable

@Serializable
internal data class AttachmentResponse(
    val results: List<Attachment>,
) {
    @Serializable
    internal data class Attachment(
        val id: String,
        val title: String,
    )
}
