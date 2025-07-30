package ru.kode.android.build.publish.plugin.slack.task.distribution.entity

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class SlackGetUploadUrlResponse(
    override val ok: Boolean,
    override val error: String?,
    @Json(name = "upload_url") val uploadUrl: String?,
    @Json(name = "file_id") val fileId: String?,
) : BaseSlackResponse
