package ru.kode.android.build.publish.plugin.slack.network.entity

import kotlinx.serialization.Serializable

@Serializable
@Suppress("ConstructorParameterNaming") // network model
internal data class SlackChangelogBody(
    val icon_url: String,
    val username: String,
    val blocks: List<Block>,
    val attachments: List<Attachment>,
) {
    @Serializable
    data class Attachment(
        val color: String,
        val blocks: List<Block>,
    )

    @Serializable
    data class Block(
        val type: String,
        val text: Text,
    )

    @Serializable
    data class Text(
        val type: String,
        val text: String,
    )
}
