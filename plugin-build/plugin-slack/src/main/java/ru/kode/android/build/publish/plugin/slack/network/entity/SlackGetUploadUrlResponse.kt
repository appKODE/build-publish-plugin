package ru.kode.android.build.publish.plugin.slack.network.entity

import kotlinx.serialization.Serializable

/**
 * Response from Slack `files.getUploadURLExternal`.
 */
@Serializable
internal data class SlackGetUploadUrlResponse(
    val ok: Boolean,
    val error: String? = null,
    val upload_url: String? = null,
    val file_id: String? = null,
)
