package ru.kode.android.build.publish.plugin.slack.controller

import kotlinx.serialization.json.Json
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import ru.kode.android.build.publish.plugin.core.enity.IssueSource
import ru.kode.android.build.publish.plugin.core.logger.PluginLogger
import ru.kode.android.build.publish.plugin.core.util.UploadError
import ru.kode.android.build.publish.plugin.core.util.createPartFromString
import ru.kode.android.build.publish.plugin.core.util.ellipsizeAt
import ru.kode.android.build.publish.plugin.core.util.executeNoResult
import ru.kode.android.build.publish.plugin.core.util.executeWithResult
import ru.kode.android.build.publish.plugin.core.zip.zipped
import ru.kode.android.build.publish.plugin.slack.messages.blockTextHasMoreSymbolsMessage
import ru.kode.android.build.publish.plugin.slack.messages.failedToSendChangelogMessage
import ru.kode.android.build.publish.plugin.slack.messages.headerTextHasMoreSymbolsMessage
import ru.kode.android.build.publish.plugin.slack.messages.sendingChangelogMessage
import ru.kode.android.build.publish.plugin.slack.network.SlackApi
import ru.kode.android.build.publish.plugin.slack.network.SlackUploadApi
import ru.kode.android.build.publish.plugin.slack.network.entity.SlackChangelogBody
import ru.kode.android.build.publish.plugin.slack.network.entity.UploadingFileRequest
import java.io.File

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
    private val logger: PluginLogger,
) : SlackController {
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

            val files =
                json.encodeToString(
                    listOf(UploadingFileRequest(fileId, zippedFile.name)),
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

    override fun send(message: SlackMessage) {
        val changelogMessages =
            message.text.formatIssues(message.issueSources)
                .split("\n")
                .filter { it.isNotBlank() }
        val splitAttachmentBlocks =
            listOfNotNull(buildSectionBlock(message.userMentions.joinToString(", ")))
                .plus(changelogMessages.mapNotNull { buildSectionBlock(it) })
                .chunked(MAX_ATTACHMENTS_COUNT)

        val changelogBody =
            SlackChangelogBody(
                icon_url = message.iconUrl,
                username = BUILD_BOT_NAME,
                blocks = listOf(buildHeaderBlock(message.header)),
                attachments =
                    splitAttachmentBlocks.map { blocks ->
                        SlackChangelogBody.Attachment(
                            color = message.attachmentColor,
                            blocks = blocks,
                        )
                    },
            )
        logger.info(sendingChangelogMessage(changelogBody.toString(), message.webhookUrl))
        slackApi
            .send(message.webhookUrl, changelogBody)
            .executeNoResult()
            .onFailure { logger.error(failedToSendChangelogMessage(message.webhookUrl), it) }
    }

    private fun buildHeaderBlock(text: String): SlackChangelogBody.Block =
        SlackChangelogBody.Block(
            type = BLOCK_TYPE_HEADER,
            text =
                SlackChangelogBody.Text(
                    type = TEXT_TYPE_PLAIN_TEXT,
                    text =
                        text.also {
                            if (it.length > MAX_BLOCK_SYMBOLS) {
                                logger.info(headerTextHasMoreSymbolsMessage(MAX_BLOCK_SYMBOLS))
                            }
                        }.ellipsizeAt(MAX_BLOCK_SYMBOLS),
                ),
        )

    private fun buildSectionBlock(
        text: String,
        textType: String = TEXT_TYPE_MARKDOWN,
    ): SlackChangelogBody.Block? {
        if (text.isBlank()) return null
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

    private fun String.formatIssues(issueSources: List<IssueSource>): String {
        var result = this
        issueSources.forEach { source ->
            if (source.numberPattern.isBlank()) return@forEach
            result =
                Regex(source.numberPattern).replace(result) { match ->
                    val key = match.value
                    val url = source.urlPrefix
                    if (url.isNullOrBlank()) key else "<$url$key|$key>"
                }
        }
        return result
    }
}

private fun getAuthorisationHeader(token: String): String = "Bearer $token"
