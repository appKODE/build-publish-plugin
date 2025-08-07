package ru.kode.android.build.publish.plugin.slack.service.webhook

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import ru.kode.android.build.publish.plugin.core.util.ellipsizeAt
import ru.kode.android.build.publish.plugin.core.util.executeOrThrow
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

abstract class SlackWebhookService
    @Inject
    constructor() : BuildService<SlackWebhookService.Params> {
        interface Params : BuildServiceParameters {
            val webhookUrl: Property<String>
            val networkService: Property<SlackNetworkService>
        }

        internal abstract val apiProperty: Property<SlackApi>

        init {
            apiProperty.set(
                parameters.networkService.get().retrofitProperty.map { retrofit ->
                    retrofit.baseUrl(STUB_BASE_URL).build()
                        .create(SlackApi::class.java)
                },
            )
        }

        private val api: SlackApi get() = apiProperty.get()
        private val webhookUrl: String get() = parameters.webhookUrl.get()

        /**
         * Sends url formatted data to webhook at [webhookUrl]
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
            api.send(webhookUrl, changelogBody).executeOrThrow()
        }

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

        companion object {
            private val logger: Logger = Logging.getLogger(SlackUploadService::class.java)
        }
    }
