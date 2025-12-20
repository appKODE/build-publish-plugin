package ru.kode.android.build.publish.plugin.confluence.network.entity

import kotlinx.serialization.Serializable

@Serializable
internal data class CommentResponse(
    val results: List<Comment>,
) {
    @Serializable
    internal data class Comment(
        val id: String,
        val body: CommentBody,
    )

    @Serializable
    internal data class CommentBody(
        val storage: Storage,
    )

    @Serializable
    internal data class Storage(
        val value: String,
        val representation: String? = null,
    )
}
