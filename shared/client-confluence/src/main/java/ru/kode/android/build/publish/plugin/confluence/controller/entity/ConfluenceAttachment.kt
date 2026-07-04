package ru.kode.android.build.publish.plugin.confluence.controller.entity

/**
 * Represents an attachment on a Confluence page.
 *
 * @property id The ID of the attachment.
 * @property fileName The name of the file attached.
 */
data class ConfluenceAttachment(
    val id: String,
    val fileName: String,
)
