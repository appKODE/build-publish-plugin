package ru.kode.android.build.publish.plugin.slack.controller

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.gradle.api.logging.Logger
import ru.kode.android.build.publish.plugin.core.util.UploadError
import ru.kode.android.build.publish.plugin.core.util.createPartFromString
import ru.kode.android.build.publish.plugin.core.util.ellipsizeAt
import ru.kode.android.build.publish.plugin.core.util.executeNoResult
import ru.kode.android.build.publish.plugin.core.util.executeWithResult
import ru.kode.android.build.publish.plugin.core.zip.zipped
import ru.kode.android.build.publish.plugin.slack.messages.blockTextHasMoreSymbolsMessage
import ru.kode.android.build.publish.plugin.slack.messages.failedToSendChangelogMessage
import ru.kode.android.build.publish.plugin.slack.messages.headerTextHasMoreSymbolsMessage
import ru.kode.android.build.publish.plugin.slack.network.SlackApi
import ru.kode.android.build.publish.plugin.slack.network.SlackUploadApi
import ru.kode.android.build.publish.plugin.slack.network.entity.SlackChangelogBody
import ru.kode.android.build.publish.plugin.slack.network.entity.UploadingFileRequest
import java.io.File
import kotlin.collections.joinToString

private const val BLOCK_TYPE_HEADER = "header"
private const val BLOCK_TYPE_SECTION = "section"
private const val TEXT_TYPE_MARKDOWN = "mrkdwn"
private const val TEXT_TYPE_PLAIN_TEXT = "plain_text"
private const val MAX_BLOCK_SYMBOLS = 3000
private const val MAX_ATTACHMENTS_COUNT = 50
private const val BUILD_BOT_NAME = "buildBot"

internal class SlackControllerImpl(
    private val json: Json,
    private val slackApi: SlackApi,
    private val uploadApi: SlackUploadApi,
    private val logger: Logger
) : SlackController {

    /**
     * Uploads a file to Slack and shares it in the specified channels.
     *
     * This method performs a multi-step upload process:
     * 1. Requests an upload URL from Slack
     * 2. Uploads the file content to the provided URL
     * 3. Completes the upload and shares the file in the specified channels
     *
     * @param initialComment The base name to use for the uploaded file
     * @param file The file to upload
     * @param channels Set of channel IDs or names where the file should be shared
     *
     * @throws UploadError if any step of the upload process fails
     */
    override fun upload(
        uploadToken: String,
        initialComment: String,
        file: File,
        channels: List<String>,
    ) {
        val zippedFile = file.zipped()
        val getUrlResponse =
            uploadApi
                .getUploadUrl(
                    authorisation = getAuthorisationHeader(uploadToken),
                    fileName = zippedFile.name,
                    length = zippedFile.length(),
                )
                .executeWithResult()
                .getOrThrow()

        if (getUrlResponse.ok) {
            val url = requireNotNull(getUrlResponse.upload_url)
            val fileId = requireNotNull(getUrlResponse.file_id)
            val filePart =
                MultipartBody.Part.createFormData(
                    "file",
                    zippedFile.name,
                    zippedFile.asRequestBody(),
                )
            uploadApi
                .upload(
                    authorisation = getAuthorisationHeader(uploadToken),
                    url = url,
                    fileName = createPartFromString(zippedFile.name),
                    filePart = filePart,
                )
                .executeWithResult()
                .getOrThrow()

            val files = json.encodeToString(
                listOf(UploadingFileRequest(fileId, zippedFile.name))
            )
            uploadApi
                .completeUploading(
                    authorisation = getAuthorisationHeader(uploadToken),
                    files = files,
                    channels = channels.joinToString(),
                    initialComment = initialComment,
                )
                .executeWithResult()
                .getOrThrow()
        } else {
            throw UploadError(requireNotNull(getUrlResponse.error))
        }
    }

    /**
     * Sends a formatted message to the configured Slack webhook.
     *
     * This method:
     * 1. Formats the message with the provided parameters
     * 2. Handles message chunking if it exceeds Slack's size limits
     * 3. Sends the message to the configured webhook URL
     *
     * @param initialComment The base name for the build being reported
     * @param changelog The changelog text to include in the message
     * @param userMentions Optional user mentions to include in the message
     * @param iconUrl URL of the icon to display with the message
     * @param attachmentColor Color code for the message attachment (e.g., "#36a64f" for green)
     *
     * @throws Exception if the webhook request fails
     */
    override fun send(
        webhookUrl: String,
        initialComment: String,
        changelog: String,
        userMentions: List<String>,
        iconUrl: String,
        attachmentColor: String,
        issueUrlPrefix: String,
        issueNumberPattern: String,
    ) {
        val changelogMessages =
            changelog.formatIssues(issueUrlPrefix, issueNumberPattern)
                .split("\n")
                .filter { it.isNotBlank() }
        val splitAttachmentBlocks =
            listOf(buildSectionBlock(userMentions.joinToString(", ")))
                .plus(changelogMessages.map { buildSectionBlock(it) })
                .chunked(MAX_ATTACHMENTS_COUNT)

        val changelogBody =
            SlackChangelogBody(
                icon_url = iconUrl,
                username = BUILD_BOT_NAME,
                blocks = listOf(buildHeaderBlock(initialComment)),
                attachments =
                    splitAttachmentBlocks.map { blocks ->
                        SlackChangelogBody.Attachment(
                            color = attachmentColor,
                            blocks = blocks,
                        )
                    },
            )
        logger.info("sending $changelogBody to $webhookUrl")
        slackApi
            .send(webhookUrl, changelogBody)
            .executeNoResult()
            .onFailure { logger.error(failedToSendChangelogMessage(webhookUrl), it) }
    }

    /**
     * Creates a header block for the Slack message.
     *
     * @param text The text to display in the header
     *
     * @return A formatted header block
     */
    private fun buildHeaderBlock(text: String): SlackChangelogBody.Block {
        return SlackChangelogBody.Block(
            type = BLOCK_TYPE_HEADER,
            text =
                SlackChangelogBody.Text(
                    // header can have plain_text type only
                    type = TEXT_TYPE_PLAIN_TEXT,
                    text =
                        text.also {
                            if (it.length > MAX_BLOCK_SYMBOLS) {
                                logger.info(headerTextHasMoreSymbolsMessage(MAX_BLOCK_SYMBOLS))
                            }
                        }.ellipsizeAt(MAX_BLOCK_SYMBOLS),
                ),
        )
    }

    private fun buildSectionBlock(
        text: String,
        textType: String = TEXT_TYPE_MARKDOWN,
    ): SlackChangelogBody.Block {
        return SlackChangelogBody.Block(
            type = BLOCK_TYPE_SECTION,
            text =
                SlackChangelogBody.Text(
                    type = textType,
                    text =
                        text.also {
                            if (it.length > MAX_BLOCK_SYMBOLS) {
                                logger.info(blockTextHasMoreSymbolsMessage(MAX_BLOCK_SYMBOLS))
                            }
                        }.ellipsizeAt(MAX_BLOCK_SYMBOLS),
                ),
        )
    }


    /**
     * Formats issue references in the changelog text.
     *
     * This method transforms raw issue numbers into clickable links using the
     * configured issue URL prefix and pattern.
     *
     * @receiver The raw changelog text
     *
     * @return The formatted changelog with issue links
     */
    private fun String.formatIssues(issueUrlPrefix: String, issueNumberPattern: String): String {
        return this.replace(
            Regex(issueNumberPattern),
            "<$issueUrlPrefix\$0|\$0>"
        )
    }
}

private fun getAuthorisationHeader(token: String): String {
    return "Bearer $token"
}
