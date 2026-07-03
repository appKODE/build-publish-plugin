package ru.kode.android.build.publish.plugin.sender.task.nextcloud.work

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.nextcloud.controller.factory.NextcloudControllerFactory
import javax.inject.Inject

internal interface UploadToNextcloudParameters : WorkParameters {
    val baseUrl: Property<String>
    val username: Property<String>
    val password: Property<String>
    val file: RegularFileProperty
    val remotePath: Property<String>
    val remoteFileName: Property<String>
}

internal abstract class UploadToNextcloudWork
    @Inject
    constructor() : WorkAction<UploadToNextcloudParameters> {
        override fun execute() {
            NextcloudControllerFactory.build(
                baseUrl = parameters.baseUrl.get(),
                username = parameters.username.get(),
                password = parameters.password.get(),
            ).uploadFile(
                remotePath = parameters.remotePath.get(),
                remoteFileName = parameters.remoteFileName.get(),
                file = parameters.file.asFile.get(),
            )
        }
    }
