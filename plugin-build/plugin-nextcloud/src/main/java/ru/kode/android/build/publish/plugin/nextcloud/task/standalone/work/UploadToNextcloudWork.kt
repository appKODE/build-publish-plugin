package ru.kode.android.build.publish.plugin.nextcloud.task.standalone.work

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import ru.kode.android.build.publish.plugin.core.task.ServiceWorkParameters
import ru.kode.android.build.publish.plugin.nextcloud.messages.uploadingToNextcloudMessage
import ru.kode.android.build.publish.plugin.nextcloud.service.NextcloudService
import javax.inject.Inject

internal interface UploadToNextcloudParameters : ServiceWorkParameters {
    val file: RegularFileProperty
    val remotePath: Property<String>
    val remoteFileName: Property<String>
    val service: Property<NextcloudService>
}

internal abstract class UploadToNextcloudWork
    @Inject
    constructor() : WorkAction<UploadToNextcloudParameters> {
        override fun execute() {
            val file = parameters.file.asFile.get()
            val remotePath = parameters.remotePath.get()
            val remoteFileName = parameters.remoteFileName.get()
            parameters.loggerService.get().info(uploadingToNextcloudMessage(file.name, remotePath, remoteFileName))
            parameters.service.get().uploadFile(
                remotePath = remotePath,
                remoteFileName = remoteFileName,
                file = file,
            )
        }
    }
