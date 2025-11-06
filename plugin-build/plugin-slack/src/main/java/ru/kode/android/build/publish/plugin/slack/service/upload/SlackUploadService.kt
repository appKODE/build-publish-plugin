package ru.kode.android.build.publish.plugin.slack.service.upload

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import ru.kode.android.build.publish.plugin.core.util.UploadException
import ru.kode.android.build.publish.plugin.core.util.createPartFromString
import ru.kode.android.build.publish.plugin.core.util.executeOrThrow
import ru.kode.android.build.publish.plugin.slack.service.network.SlackNetworkService
import ru.kode.android.build.publish.plugin.slack.task.distribution.api.SlackUploadApi
import ru.kode.android.build.publish.plugin.slack.task.distribution.entity.SlackRichTextBlock
import ru.kode.android.build.publish.plugin.slack.task.distribution.entity.SlackRichTextElement
import ru.kode.android.build.publish.plugin.slack.task.distribution.entity.SlackTextStyle
import ru.kode.android.build.publish.plugin.slack.task.distribution.entity.UploadingFileRequest
import java.io.File
import java.nio.charset.StandardCharsets
import javax.inject.Inject

private const val SLACK_BASE_URL = "https://slack.com/api/"
private const val MAX_BLOCK_SYMBOLS = 3000

// Slack API limit for blocks JSON is approximately 50KB
// We use 45KB as a safe margin to account for JSON structure overhead
private const val MAX_BLOCKS_JSON_SIZE_BYTES = 45 * 1024

/**
 * A service that handles file uploads to Slack using the Slack Files API.
 *
 * This service provides functionality to:
 * - Obtain upload URLs from Slack
 * - Upload files to Slack
 * - Complete the upload process and share files in channels
 * - Handle authentication and error cases
 *
 * It's designed to work with the Slack API's file upload flow, which involves
 * multiple steps to support large file uploads.
 */
abstract class SlackUploadService
    @Inject
    constructor() : BuildService<SlackUploadService.Params> {
        /**
         * Configuration parameters for the SlackUploadService.
         */
        interface Params : BuildServiceParameters {
            /**
             * File containing the Slack API token for file uploads
             */
            val uploadApiTokenFile: RegularFileProperty

            /**
             * The network service to use for HTTP operations
             */
            val networkService: Property<SlackNetworkService>
        }

        internal abstract val uploadApiProperty: Property<SlackUploadApi>

        init {
            uploadApiProperty.set(
                parameters.networkService.flatMap { it.retrofitProperty }.map { retrofit ->
                    retrofit.baseUrl(SLACK_BASE_URL).build()
                        .create(SlackUploadApi::class.java)
                },
            )
        }

        private val moshi: Moshi get() = parameters.networkService.flatMap { it.moshiProperty }.get()
        private val uploadApi: SlackUploadApi get() = uploadApiProperty.get()
        private val token: String get() = parameters.uploadApiTokenFile.get().asFile.readText()

        /**
         * Uploads a file to Slack and shares it in the specified channels with changelog.
         *
         * This method performs a multistep upload process:
         * 1. Requests an upload URL from Slack
         * 2. Uploads the file content to the provided URL
         * 3. Completes the upload and shares the file in the specified channels
         *
         * @param baseOutputFileName The base name to use for the uploaded file
         * @param buildName The build name or identifier to include in the comment
         * @param file The file to upload
         * @param channels Set of channel IDs or names where the file should be shared
         * @param changelog The changelog text to format as list
         * @param userMentions Set of user mentions to include in the message
         * @param description The description string to display
         *
         * @throws UploadException if any step of the upload process fails
         */
        fun upload(
            baseOutputFileName: String,
            buildName: String,
            file: File,
            channels: Set<String>,
            changelog: String? = null,
            userMentions: Set<String> = emptySet(),
            description: String? = null,
        ) {
            val getUrlResponse =
                uploadApi
                    .getUploadUrl(
                        authorisation = getAuthorisationHeader(token),
                        fileName = file.name,
                        length = file.length(),
                    )
                    .executeOrThrow()

            if (getUrlResponse.ok) {
                val url = requireNotNull(getUrlResponse.uploadUrl)
                val fileId = requireNotNull(getUrlResponse.fileId)
                val filePart =
                    MultipartBody.Part.createFormData(
                        "file",
                        file.name,
                        file.asRequestBody(),
                    )
                uploadApi
                    .upload(
                        authorisation = getAuthorisationHeader(token),
                        url = url,
                        fileName = createPartFromString(file.name),
                        filePart = filePart,
                    )
                    .executeOrThrow()
                val filesAdapter = moshi.adapter<List<UploadingFileRequest>>()
                val files = filesAdapter.toJson(listOf(UploadingFileRequest(fileId, file.name)))

                val blocks =
                    createRichTextBlocks(
                        baseOutputFileName = baseOutputFileName,
                        buildName = buildName,
                        changelog = changelog,
                        userMentions = userMentions,
                        description = description,
                    )

                uploadApi.completeUploading(
                    authorisation = getAuthorisationHeader(token),
                    files = files,
                    channels = channels.joinToString(),
                    blocks = blocks,
                ).executeOrThrow()
            } else {
                throw UploadException(requireNotNull(getUrlResponse.error))
            }
        }

        /**
         * Creates 'rich text blocks' for Slack message based on the provided parameters.
         * Structure matches the format shown in the reference image:
         * - Header with app name and build name (always included)
         * - User mentions section (optional)
         * - description from Tag creation (optional)
         * - Changelog as bulleted list (optional)
         *
         * The method always creates at least a header block with baseOutputFileName and buildName.
         */
        private fun createRichTextBlocks(
            baseOutputFileName: String,
            buildName: String,
            changelog: String?,
            userMentions: Set<String>,
            description: String?,
        ): String? {
            val elements = mutableListOf<SlackRichTextElement>()

            elements.add(
                SlackRichTextElement.SlackRichTextSection(
                    elements =
                        listOf(
                            SlackRichTextElement.SlackRichTextSectionContent.SlackText(
                                text = "$baseOutputFileName $buildName",
                                style = SlackTextStyle(bold = true),
                            ),
                        ),
                ),
            )

            if (userMentions.isNotEmpty()) {
                val mentionsText = userMentions.joinToString(", ")
                elements.add(
                    SlackRichTextElement.SlackRichTextSection(
                        elements =
                            listOf(
                                SlackRichTextElement.SlackRichTextSectionContent.SlackText(text = mentionsText),
                            ),
                    ),
                )
            }

            if (!description.isNullOrBlank()) {
                elements.add(
                    SlackRichTextElement.SlackRichTextSection(
                        elements =
                            listOf(
                                SlackRichTextElement.SlackRichTextSectionContent.SlackText(text = description),
                            ),
                    ),
                )
            }

            if (!changelog.isNullOrBlank()) {
                val changelogLines =
                    changelog
                        .split("\n")
                        .mapNotNull { line ->
                            if (line.isBlank()) return@mapNotNull null
                            // Remove bullet point prefix (•) if present, as Slack rich_text_list adds its own bullets
                            val trimmedLine = line.trim().removePrefix("•").trim()
                            val truncatedLine =
                                if (trimmedLine.length > MAX_BLOCK_SYMBOLS) {
                                    trimmedLine.take(MAX_BLOCK_SYMBOLS - 3) + "..."
                                } else {
                                    trimmedLine
                                }
                            SlackRichTextElement.SlackRichTextSection(
                                elements =
                                    listOf(
                                        SlackRichTextElement.SlackRichTextSectionContent.SlackText(
                                            text = truncatedLine,
                                        ),
                                    ),
                            )
                        }

                if (changelogLines.isNotEmpty()) {
                    elements.add(
                        SlackRichTextElement.SlackRichTextList(
                            elements = changelogLines,
                            style = "bullet",
                        ),
                    )
                }
            }

            val richTextBlock =
                SlackRichTextBlock(
                    elements = elements,
                )

            val blocksAdapter = moshi.adapter<List<SlackRichTextBlock>>()
            val blocksJson = blocksAdapter.toJson(listOf(richTextBlock))

            // Ensure blocks JSON doesn't exceed Slack API limits
            // If it does, truncate changelog and regenerate
            return if (blocksJson.toByteArray(StandardCharsets.UTF_8).size > MAX_BLOCKS_JSON_SIZE_BYTES) {
                // Truncate changelog by removing last items until we fit within limit
                val truncatedElements = truncateElementsToFitLimit(elements, blocksAdapter)
                val truncatedBlock =
                    SlackRichTextBlock(
                        elements = truncatedElements,
                    )
                blocksAdapter.toJson(listOf(truncatedBlock))
            } else {
                blocksJson
            }
        }

        /**
         * Truncates elements to fit within Slack API size limits by removing changelog items
         * from the end until the blocks JSON is within the limit.
         *
         * only truncating the changelog list if present.
         */
        private fun truncateElementsToFitLimit(
            elements: List<SlackRichTextElement>,
            blocksAdapter: JsonAdapter<List<SlackRichTextBlock>>,
        ): List<SlackRichTextElement> {
            val changelogListIndex = elements.indexOfLast { it is SlackRichTextElement.SlackRichTextList }
            if (changelogListIndex == -1) {
                return elements
            }

            val truncatedElements = elements.toMutableList()
            val changelogList = truncatedElements[changelogListIndex] as SlackRichTextElement.SlackRichTextList

            var currentList = changelogList
            while (currentList.elements.isNotEmpty()) {
                val testElements = truncatedElements.toMutableList()
                testElements[changelogListIndex] = currentList
                val testBlock = SlackRichTextBlock(elements = testElements)
                val testJson = blocksAdapter.toJson(listOf(testBlock))
                if (testJson.toByteArray(StandardCharsets.UTF_8).size <= MAX_BLOCKS_JSON_SIZE_BYTES) {
                    truncatedElements[changelogListIndex] = currentList
                    break
                }

                currentList =
                    SlackRichTextElement.SlackRichTextList(
                        elements = currentList.elements.dropLast(1),
                        style = currentList.style,
                    )
            }

            if (currentList.elements.isEmpty()) {
                truncatedElements.removeAt(changelogListIndex)
            } else {
                truncatedElements[changelogListIndex] = currentList
            }

            return truncatedElements.toList()
        }
    }

private fun getAuthorisationHeader(token: String): String {
    return "Bearer $token"
}
