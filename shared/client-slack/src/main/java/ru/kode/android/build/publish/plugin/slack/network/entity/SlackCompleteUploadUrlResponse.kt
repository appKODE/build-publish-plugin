package ru.kode.android.build.publish.plugin.slack.network.entity

import kotlinx.serialization.Serializable

@Serializable
internal data class SlackCompleteUploadUrlResponse(
    val ok: Boolean,
    val error: String? = null,
)
