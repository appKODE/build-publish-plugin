package ru.kode.android.build.publish.plugin.yandexdisk.task.standalone.work

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import ru.kode.android.build.publish.plugin.core.task.ServiceWorkParameters
import ru.kode.android.build.publish.plugin.yandexdisk.messages.uploadingToYandexDiskMessage
import ru.kode.android.build.publish.plugin.yandexdisk.service.YandexDiskService
import javax.inject.Inject

internal interface UploadToYandexDiskParameters : ServiceWorkParameters {
    val file: RegularFileProperty
    val remotePath: Property<String>
    val remoteFileName: Property<String>
    val service: Property<YandexDiskService>
}

internal abstract class UploadToYandexDiskWork
    @Inject
    constructor() :
    WorkAction<UploadToYandexDiskParameters> {
        override fun execute() {
            val file = parameters.file.asFile.get()
            val remotePath = parameters.remotePath.get()
            val remoteFileName = parameters.remoteFileName.get()
            parameters.loggerService.get().info(uploadingToYandexDiskMessage(file.name, remotePath, remoteFileName))
            parameters.service.get().uploadFile(
                remotePath = remotePath,
                remoteFileName = remoteFileName,
                file = file,
            )
        }
    }
