package ru.kode.android.build.publish.plugin.nextcloud.task.distribution.work

import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.core.logger.LoggerService
import ru.kode.android.build.publish.plugin.core.zip.zipped
import ru.kode.android.build.publish.plugin.nextcloud.config.NextcloudShareMode
import ru.kode.android.build.publish.plugin.nextcloud.messages.internalShareReadyMessage
import ru.kode.android.build.publish.plugin.nextcloud.messages.noInternalRecipientsConfiguredMessage
import ru.kode.android.build.publish.plugin.nextcloud.messages.shareCreatedMessage
import ru.kode.android.build.publish.plugin.nextcloud.messages.shareReusedMessage
import ru.kode.android.build.publish.plugin.nextcloud.messages.uploadFailedMessage
import ru.kode.android.build.publish.plugin.nextcloud.service.NextcloudService

interface NextcloudUploadParameters : WorkParameters {
    val outputFile: RegularFileProperty
    val remotePath: Property<String>
    val remoteFileName: Property<String>
    val shareMode: Property<NextcloudShareMode>
    val userRecipients: SetProperty<String>
    val groupRecipients: SetProperty<String>
    val service: Property<NextcloudService>
    val loggerService: Property<LoggerService>
    val compressed: Property<Boolean>
}

internal abstract class NextcloudUploadWork : WorkAction<NextcloudUploadParameters> {
    override fun execute() {
        val service = parameters.service.get()
        val logger = parameters.loggerService.get()
        val compressed = parameters.compressed.orElse(false).get()
        val outputFile = parameters.outputFile.asFile.get()
        val distributionFile = if (compressed) outputFile.zipped() else outputFile
        val remotePath = parameters.remotePath.get()
        val remoteFileName = parameters.remoteFileName.get()
        val shareMode = parameters.shareMode.orElse(NextcloudShareMode.INTERNAL_RECIPIENTS).get()
        val userRecipients = parameters.userRecipients.get()
        val groupRecipients = parameters.groupRecipients.get()

        if (
            shareMode == NextcloudShareMode.INTERNAL_RECIPIENTS &&
            userRecipients.isEmpty() &&
            groupRecipients.isEmpty()
        ) {
            throw GradleException(noInternalRecipientsConfiguredMessage())
        }

        runCatching {
            service.uploadFile(
                remotePath = remotePath,
                remoteFileName = remoteFileName,
                file = distributionFile,
            )
            val shareResult =
                service.shareFile(
                    remotePath = remotePath,
                    remoteFileName = remoteFileName,
                    shareMode = shareMode,
                    userRecipients = userRecipients,
                    groupRecipients = groupRecipients,
                )
            val remoteFilePath =
                service.resolveRemoteFilePath(
                    remotePath = remotePath,
                    fileName = remoteFileName,
                )

            if (shareResult.mode == NextcloudShareMode.PUBLIC_LINK) {
                if (shareResult.reusedCount > 0) {
                    logger.quiet(shareReusedMessage(remoteFilePath, shareResult.linkUrl))
                } else {
                    logger.quiet(shareCreatedMessage(remoteFilePath, shareResult.linkUrl))
                }
            } else {
                logger.quiet(
                    internalShareReadyMessage(
                        remoteFilePath = remoteFilePath,
                        internalUrl = shareResult.linkUrl,
                        createdCount = shareResult.createdCount,
                        reusedCount = shareResult.reusedCount,
                    ),
                )
            }
        }.onFailure { ex ->
            logger.error(uploadFailedMessage(), ex)
        }.getOrThrow()
    }
}
