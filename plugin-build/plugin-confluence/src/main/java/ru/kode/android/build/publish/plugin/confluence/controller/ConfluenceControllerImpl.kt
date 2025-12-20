package ru.kode.android.build.publish.plugin.confluence.controller

import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import ru.kode.android.build.publish.plugin.confluence.controller.entity.ConfluenceAttachment
import ru.kode.android.build.publish.plugin.confluence.controller.entity.ConfluenceComment
import ru.kode.android.build.publish.plugin.confluence.network.api.ConfluenceApi
import ru.kode.android.build.publish.plugin.confluence.network.entity.AddCommentRequest
import ru.kode.android.build.publish.plugin.core.util.executeNoResult
import ru.kode.android.build.publish.plugin.core.util.executeWithResult
import java.io.File

/**
 * Controller for interacting with the Jira API.
 *
 * @param api The Jira API implementation
 */
internal class ConfluenceControllerImpl(
    private val baseUrl: String,
    private val api: ConfluenceApi,
) : ConfluenceController {
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
    override fun uploadFile(
        pageId: String,
        file: File,
    ) {
        val filePart =
            MultipartBody.Part.createFormData(
                "file",
                file.name,
                file.asRequestBody(),
            )
        api
            .uploadAttachment(
                pageId = pageId,
                file = filePart,
            )
            .executeNoResult()
            .getOrThrow()
    }

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
    override fun addComment(
        pageId: String,
        fileName: String,
    ) {
        val comment = "<a href=\"$baseUrl/download/attachments/$pageId/$fileName\">$fileName</a>"
        api
            .addComment(
                commentRequest =
                    AddCommentRequest(
                        type = "comment",
                        container = AddCommentRequest.Container(pageId, "page"),
                        body =
                            AddCommentRequest.Body(
                                AddCommentRequest.Storage(comment, "storage"),
                            ),
                    ),
            )
            .executeNoResult()
            .getOrThrow()
    }

    /**
     * Retrieves a list of attachments for a given Confluence page.
     *
     * @param pageId The ID of the Confluence page to retrieve attachments for.
     * @return A list of [ConfluenceAttachment] objects representing the attachments on the page.
     */
    override fun getAttachments(pageId: String): List<ConfluenceAttachment> {
        return api.getAttachments(pageId)
            .executeWithResult()
            .getOrThrow()
            .results
            .map {
                ConfluenceAttachment(
                    id = it.id,
                    fileName = it.title,
                )
            }
    }

    /**
     * Retrieves a list of comments for a given Confluence page.
     *
     * @param pageId The ID of the Confluence page to retrieve comments for.
     * @return A list of [ConfluenceComment] objects representing the comments on the page.
     */
    override fun getComments(pageId: String): List<ConfluenceComment> {
        return api.getComments(pageId)
            .executeWithResult()
            .getOrThrow()
            .results
            .map {
                ConfluenceComment(
                    id = it.id,
                    html = it.body.storage.value,
                )
            }
    }

    /**
     * Removes an attachment from Confluence.
     *
     * @param attachmentId The ID of the attachment to delete
     *
     * @throws IllegalArgumentException if attachmentId is blank
     * @throws Exception if the API request fails
     */
    override fun removeAttachment(attachmentId: String) {
        api
            .deleteContent(attachmentId)
            .executeNoResult()
    }

    /**
     * Removes a comment from a Confluence page.
     *
     * @param commentId The ID of the comment to delete
     *
     * @throws IllegalArgumentException if commentId is blank
     * @throws Exception if the API request fails
     */
    override fun removeComment(commentId: String) {
        api
            .deleteContent(commentId)
            .executeNoResult()
    }
}
