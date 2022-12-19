package ru.kode.android.build.publish.plugin.task.slack.changelog.entity

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Suppress("ConstructorParameterNaming") // network model
internal data class SlackChangelogBody(
    val icon_url: String,
    val username: String,
    val blocks: List<Block>
) {
    @JsonClass(generateAdapter = true)
    data class Block(
        val type: String,
        val text: Text
    )

    @JsonClass(generateAdapter = true)
    data class Text(
        val type: String,
        val text: String
    )
}
