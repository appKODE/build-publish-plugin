package ru.kode.android.build.publish.plugin.confluence.network.entity

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class CommentResponse(
    val results: List<Comment>
) {
    @JsonClass(generateAdapter = true)
    internal data class Comment(
        val id: String,
        val body: CommentBody
    )

    @JsonClass(generateAdapter = true)
    internal data class CommentBody(
        val storage: Storage
    )

    @JsonClass(generateAdapter = true)
    internal data class Storage(
        val value: String,
        val representation: String? = null,
    )
}