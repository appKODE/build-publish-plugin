package ru.kode.android.build.publish.plugin.slack.network.entity

import kotlinx.serialization.Serializable

/**
 * Request payload used when completing an external upload.
 */
@Serializable
internal data class UploadingFileRequest(
    val id: String,
    val title: String,
)
