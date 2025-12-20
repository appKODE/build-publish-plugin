package ru.kode.android.build.publish.plugin.confluence.controller

import ru.kode.android.build.publish.plugin.confluence.controller.entity.ConfluenceAttachment
import ru.kode.android.build.publish.plugin.confluence.controller.entity.ConfluenceComment
import java.io.File

/**
 * Controller for interacting with the Confluence API.
 */
interface ConfluenceController {
    /**
     * Uploads a file as an attachment to a Confluence page.
     *
     * This method handles the multipart form data upload process to the Confluence REST API.
     * It will throw an exception if the upload fails for any reason.
     *
     * @param pageId The ID of the Confluence page where the file should be attached
     * @param file The file to upload. Must be a valid, readable file.
     *
     * @throws IllegalStateException if the file doesn't exist or is not readable
     * @throws Exception if the API request fails or returns an error
     */
    fun uploadFile(
        pageId: String,
        file: File,
    )

    /**
     * Adds a comment with a file download link to a Confluence page.
     *
     * The comment will include a direct download link to the attached file.
     * The file must have been previously uploaded to the page.
     *
     * @param pageId The ID of the Confluence page where the comment should be added
     * @param fileName The name of the file to create a download link for
     *
     * @throws IllegalArgumentException if the pageId is empty or invalid
     * @throws Exception if the API request fails or returns an error
     */
    fun addComment(
        pageId: String,
        fileName: String,
    )

    /**
     * Retrieves a list of attachments for a given Confluence page.
     *
     * @param pageId The ID of the Confluence page to retrieve attachments for.
     * @return A list of [ConfluenceAttachment] objects representing the attachments on the page.
     */
    fun getAttachments(pageId: String): List<ConfluenceAttachment>

    /**
     * Retrieves a list of comments for a given Confluence page.
     *
     * @param pageId The ID of the Confluence page to retrieve comments for.
     * @return A list of [ConfluenceComment] objects representing the comments on the page.
     */
    fun getComments(pageId: String): List<ConfluenceComment>

    /**
     * Removes an attachment from Confluence.
     *
     * @param attachmentId The ID of the attachment to delete
     *
     * @throws IllegalArgumentException if attachmentId is blank
     * @throws Exception if the API request fails
     */
    fun removeAttachment(attachmentId: String)

    /**
     * Removes a comment from a Confluence page.
     *
     * @param commentId The ID of the comment to delete
     *
     * @throws IllegalArgumentException if commentId is blank
     * @throws Exception if the API request fails
     */
    fun removeComment(commentId: String)
}
