package ru.kode.android.build.publish.plugin.confluence.controller.entity

/**
 * Represents a comment on a Confluence page.
 *
 * @property id The unique ID of the comment.
 * @property html The HTML content of the comment.
 */
data class ConfluenceComment(
    val id: String,
    val html: String,
)