package ru.kode.android.build.publish.plugin.nextcloud.controller.entity

import ru.kode.android.build.publish.plugin.nextcloud.config.NextcloudShareMode

data class NextcloudShare(
    val url: String,
    val reused: Boolean,
    val fileId: String? = null,
    val shareType: Int,
    val shareWith: String? = null,
)

data class NextcloudSharingResult(
    val mode: NextcloudShareMode,
    val linkUrl: String,
    val createdCount: Int,
    val reusedCount: Int,
)
