package ru.kode.android.build.publish.plugin.slack.network.entity

import kotlinx.serialization.Serializable

/**
 * Slack incoming webhook payload used for sending changelog notifications.
 */
@Serializable
@Suppress("ConstructorParameterNaming") // network model
internal data class SlackChangelogBody(
    val icon_url: String,
    val username: String,
    val blocks: List<Block>,
    val attachments: List<Attachment>,
) {
    /**
     * Slack attachment wrapper.
     */
    @Serializable
    data class Attachment(
        val color: String,
        val blocks: List<Block>,
    )

    /**
     * Slack block (e.g. header, section).
     */
    @Serializable
    data class Block(
        val type: String,
        val text: Text,
    )

    /**
     * Text payload for a [Block].
     */
    @Serializable
    data class Text(
        val type: String,
        val text: String,
    )
}
