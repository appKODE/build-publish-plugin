package ru.kode.android.build.publish.plugin.nextcloud.controller

import ru.kode.android.build.publish.plugin.nextcloud.config.NextcloudShareMode
import ru.kode.android.build.publish.plugin.nextcloud.controller.entity.NextcloudSharingResult
import java.io.File

interface NextcloudController {
    fun uploadFile(
        remotePath: String,
        remoteFileName: String,
        file: File,
    )

    fun shareFile(
        remotePath: String,
        remoteFileName: String,
        shareMode: NextcloudShareMode,
        userRecipients: Set<String>,
        groupRecipients: Set<String>,
    ): NextcloudSharingResult

    fun resolveRemoteFilePath(
        remotePath: String,
        fileName: String,
    ): String
}
