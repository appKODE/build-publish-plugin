package ru.kode.android.build.publish.plugin.slack.controller

import ru.kode.android.build.publish.plugin.core.util.UploadError
import java.io.File

interface SlackController {

    /**
     * Uploads a file to Slack and shares it in the specified channels.
     *
     * This method performs a multi-step upload process:
     * 1. Requests an upload URL from Slack
     * 2. Uploads the file content to the provided URL
     * 3. Completes the upload and shares the file in the specified channels
     *
     * @param initialComment The base name to use for the uploaded file
     * @param buildName The build name or identifier to include in the comment
     * @param file The file to upload
     * @param channels Set of channel IDs or names where the file should be shared
     *
     * @throws UploadError if any step of the upload process fails
     */
    fun upload(
        uploadToken: String,
        initialComment: String,
        file: File,
        channels: List<String>,
    )

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
    fun send(
        webhookUrl: String,
        initialComment: String,
        changelog: String,
        userMentions: List<String>,
        iconUrl: String,
        attachmentColor: String,
        issueUrlPrefix: String,
        issueNumberPattern: String
    )
}