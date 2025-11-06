package ru.kode.android.build.publish.plugin.slack.task.distribution.entity

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Represents a rich text block in Slack Block Kit.
 * https://docs.slack.dev/reference/block-kit/blocks/rich-text-block/
 */
@JsonClass(generateAdapter = true)
data class SlackRichTextBlock(
    @param:Json(name = "type") val type: String = "rich_text",
    @param:Json(name = "block_id") val blockId: String? = null,
    @param:Json(name = "elements") val elements: List<SlackRichTextElement>,
)

/**
 * Base interface for elements within a Slack rich text block.
 */
sealed interface SlackRichTextElement {
    /**
     * Represents a rich text section element.
     */
    @JsonClass(generateAdapter = true)
    data class SlackRichTextSection(
        @param:Json(name = "type") val type: String = "rich_text_section",
        @param:Json(name = "elements") val elements: List<SlackRichTextSectionContent>,
    ) : SlackRichTextElement

    /**
     * Represents a rich text list element.
     */
    @JsonClass(generateAdapter = true)
    data class SlackRichTextList(
        @param:Json(name = "type") val type: String = "rich_text_list",
        @param:Json(name = "elements") val elements: List<SlackRichTextSection>,
        @param:Json(name = "style") val style: String,
    ) : SlackRichTextElement

    /**
     * Base interface for content within a Slack rich text section.
     */
    sealed interface SlackRichTextSectionContent {
        /**
         * Represents a plain text element within a rich text section.
         */
        @JsonClass(generateAdapter = true)
        data class SlackText(
            @param:Json(name = "type") val type: String = "text",
            @param:Json(name = "text") val text: String,
            @param:Json(name = "style") val style: SlackTextStyle? = null,
        ) : SlackRichTextSectionContent
    }
}

/**
 * Represents text styling options.
 */
@JsonClass(generateAdapter = true)
data class SlackTextStyle(
    @param:Json(name = "bold") val bold: Boolean? = null,
    @param:Json(name = "italic") val italic: Boolean? = null,
    @param:Json(name = "strike") val strike: Boolean? = null,
    @param:Json(name = "code") val code: Boolean? = null,
)
