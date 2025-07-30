package ru.kode.android.build.publish.plugin.slack.task.changelog.entity

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Suppress("ConstructorParameterNaming") // network model
internal data class SlackChangelogBody(
    val icon_url: String,
    val username: String,
    val blocks: List<Block>,
    val attachments: List<Attachment>,
) {
    @JsonClass(generateAdapter = true)
    data class Attachment(
        val color: String,
        val blocks: List<Block>,
    )

    @JsonClass(generateAdapter = true)
    data class Block(
        val type: String,
        val text: Text,
    )

    @JsonClass(generateAdapter = true)
    data class Text(
        val type: String,
        val text: String,
    )
}
