package ru.kode.android.build.publish.plugin.slack.task.distribution.entity

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class SlackCompleteUploadUrlResponse(
    override val ok: Boolean,
    override val error: String?,
) : BaseSlackResponse
