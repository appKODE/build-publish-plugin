package ru.kode.android.build.publish.plugin.confluence.network.entity

import kotlinx.serialization.Serializable

/**
 * Confluence API request body for creating a comment.
 */
@Serializable
internal data class AddCommentRequest(
    val type: String,
    val container: Container,
    val body: Body,
) {
    @Serializable
    internal data class Container(
        val id: String,
        val type: String,
    )

    @Serializable
    internal data class Body(
        val storage: Storage,
    )

    @Serializable
    internal data class Storage(
        val value: String,
        val representation: String,
    )
}
