package ru.kode.android.build.publish.plugin.nextcloud.service

import ru.kode.android.build.publish.plugin.core.api.service.BasicAuthBuildService
import ru.kode.android.build.publish.plugin.core.logger.PluginLogger
import ru.kode.android.build.publish.plugin.nextcloud.config.NextcloudShareMode
import ru.kode.android.build.publish.plugin.nextcloud.controller.NextcloudController
import ru.kode.android.build.publish.plugin.nextcloud.controller.entity.NextcloudSharingResult
import ru.kode.android.build.publish.plugin.nextcloud.controller.factory.NextcloudControllerFactory
import java.io.File
import javax.inject.Inject

abstract class NextcloudService
    @Inject
    constructor() : BasicAuthBuildService<NextcloudController>() {
        override fun buildController(
            baseUrl: String,
            username: String,
            password: String,
            logger: PluginLogger,
        ): NextcloudController =
            NextcloudControllerFactory.build(
                baseUrl = baseUrl,
                username = username,
                password = password,
                logger = logger,
            )

        fun uploadFile(
            remotePath: String,
            remoteFileName: String,
            file: File,
        ) {
            controller.uploadFile(remotePath, remoteFileName, file)
        }

        fun shareFile(
            remotePath: String,
            remoteFileName: String,
            shareMode: NextcloudShareMode,
            userRecipients: Set<String>,
            groupRecipients: Set<String>,
        ): NextcloudSharingResult {
            return controller.shareFile(
                remotePath = remotePath,
                remoteFileName = remoteFileName,
                shareMode = shareMode,
                userRecipients = userRecipients,
                groupRecipients = groupRecipients,
            )
        }

        fun resolveRemoteFilePath(
            remotePath: String,
            fileName: String,
        ): String {
            return controller.resolveRemoteFilePath(remotePath, fileName)
        }
    }
