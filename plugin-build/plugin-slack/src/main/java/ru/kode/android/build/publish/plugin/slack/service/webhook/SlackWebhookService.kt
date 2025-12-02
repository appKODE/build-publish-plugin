package ru.kode.android.build.publish.plugin.slack.service.webhook

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import ru.kode.android.build.publish.plugin.core.util.ellipsizeAt
import ru.kode.android.build.publish.plugin.core.util.executeNoResult
import ru.kode.android.build.publish.plugin.slack.service.network.SlackNetworkService
import ru.kode.android.build.publish.plugin.slack.service.upload.SlackUploadService
import ru.kode.android.build.publish.plugin.slack.task.changelog.api.SlackApi
import ru.kode.android.build.publish.plugin.slack.task.changelog.entity.SlackChangelogBody
import javax.inject.Inject

private const val STUB_BASE_URL = "http://localhost/"

private const val BLOCK_TYPE_HEADER = "header"
private const val BLOCK_TYPE_SECTION = "section"
private const val TEXT_TYPE_MARKDOWN = "mrkdwn"
private const val TEXT_TYPE_PLAIN_TEXT = "plain_text"

private const val MAX_BLOCK_SYMBOLS = 3000
private const val MAX_ATTACHMENTS_COUNT = 50

private val logger: Logger = Logging.getLogger(SlackUploadService::class.java)

/**
 * A service for sending notifications to Slack using incoming webhooks.
 *
 * This service provides functionality to:
 * - Send formatted messages to Slack channels via webhooks
 * - Handle message formatting with proper blocks and attachments
 * - Manage message size limits and chunking
 * - Support both markdown and plain text formatting
 *
 * It's designed to work with Slack's Block Kit message formatting and handles
 * the complexity of splitting large messages into smaller chunks when necessary.
 */
abstract class SlackWebhookService
    @Inject
    constructor() : BuildService<SlackWebhookService.Params> {
        /**
         * Configuration parameters for the SlackWebhookService.
         */
        interface Params : BuildServiceParameters {
            /**
             * The incoming webhook URL for sending messages
             */
            val webhookUrl: Property<String>

            /**
             * The network service to use for HTTP operations
             */
            val networkService: Property<SlackNetworkService>
        }

        internal abstract val apiProperty: Property<SlackApi>

        init {
            apiProperty.set(
                parameters.networkService.flatMap { it.retrofitProperty }.map { retrofit ->
                    retrofit.baseUrl(STUB_BASE_URL).build()
                        .create(SlackApi::class.java)
                },
            )
        }

        private val api: SlackApi get() = apiProperty.get()
        private val webhookUrl: String get() = parameters.webhookUrl.get()

        /**
         * Sends a formatted message to the configured Slack webhook.
         *
         * This method:
         * 1. Formats the message with the provided parameters
         * 2. Handles message chunking if it exceeds Slack's size limits
         * 3. Sends the message to the configured webhook URL
         *
         * @param baseOutputFileName The base name for the build being reported
         * @param buildName The name or identifier of the build
         * @param changelog The changelog text to include in the message
         * @param userMentions Optional user mentions to include in the message
         * @param iconUrl URL of the icon to display with the message
         * @param attachmentColor Color code for the message attachment (e.g., "#36a64f" for green)
         *
         * @throws Exception if the webhook request fails
         */
        fun send(
            baseOutputFileName: String,
            buildName: String,
            changelog: String,
            userMentions: String,
            iconUrl: String,
            attachmentColor: String,
        ) {
            val changelogMessages =
                changelog
                    .split("\n")
                    .filter { it.isNotBlank() }
            val allAttachmentBlocks =
                listOf(buildSectionBlock(userMentions))
                    .plus(changelogMessages.map { buildSectionBlock(it) })
            val splitAttachmentBlocks =
                allAttachmentBlocks.chunked(MAX_ATTACHMENTS_COUNT)

            val changelogBody =
                SlackChangelogBody(
                    icon_url = iconUrl,
                    username = "buildBot",
                    blocks = listOf(buildHeaderBlock("$baseOutputFileName $buildName")),
                    attachments =
                        splitAttachmentBlocks.map { blocks ->
                            SlackChangelogBody.Attachment(
                                color = attachmentColor,
                                blocks = blocks,
                            )
                        },
                )
            logger.info("sending $changelogBody to $webhookUrl")
            api.send(webhookUrl, changelogBody).executeNoResult()
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
                                    logger.info("Header text has more than $MAX_BLOCK_SYMBOLS symbols")
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
                                    logger.info("Block text has more than $MAX_BLOCK_SYMBOLS symbols")
                                }
                            }.ellipsizeAt(MAX_BLOCK_SYMBOLS),
                    ),
            )
        }
    }
