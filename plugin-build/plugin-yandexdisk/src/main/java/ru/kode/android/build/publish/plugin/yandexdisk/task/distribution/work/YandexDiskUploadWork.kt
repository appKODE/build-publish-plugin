package ru.kode.android.build.publish.plugin.yandexdisk.task.distribution.work

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.core.zip.zipped
import ru.kode.android.build.publish.plugin.yandexdisk.messages.fileUploadedMessage
import ru.kode.android.build.publish.plugin.yandexdisk.messages.shareCreatedMessage
import ru.kode.android.build.publish.plugin.yandexdisk.messages.shareReusedMessage
import ru.kode.android.build.publish.plugin.yandexdisk.messages.uploadFailedMessage
import ru.kode.android.build.publish.plugin.yandexdisk.service.YandexDiskService
import ru.kode.android.gradle.commons.logger.LoggerService

interface YandexDiskUploadParameters : WorkParameters {
    val outputFile: RegularFileProperty
    val remotePath: Property<String>
    val remoteFileName: Property<String>
    val publicLink: Property<Boolean>
    val service: Property<YandexDiskService>
    val loggerService: Property<LoggerService>
    val compressed: Property<Boolean>
}

internal abstract class YandexDiskUploadWork : WorkAction<YandexDiskUploadParameters> {
    override fun execute() {
        val service = parameters.service.get()
        val logger = parameters.loggerService.get()
        val outputFile = parameters.outputFile.asFile.get()
        val distributionFile = if (parameters.compressed.orElse(false).get()) outputFile.zipped() else outputFile
        val remotePath = parameters.remotePath.get()
        val remoteFileName = parameters.remoteFileName.get()

        runCatching {
            service.uploadFile(remotePath = remotePath, remoteFileName = remoteFileName, file = distributionFile)
            val remoteFilePath = service.resolveRemoteFilePath(remotePath = remotePath, fileName = remoteFileName)

            if (parameters.publicLink.orElse(false).get()) {
                val share = service.publishFile(remotePath = remotePath, remoteFileName = remoteFileName)
                if (share.reused) {
                    logger.quiet(shareReusedMessage(remoteFilePath, share.url))
                } else {
                    logger.quiet(shareCreatedMessage(remoteFilePath, share.url))
                }
            } else {
                logger.quiet(fileUploadedMessage(remoteFilePath))
            }
        }.onFailure { ex ->
            logger.error(uploadFailedMessage(), ex)
        }.getOrThrow()
    }
}
