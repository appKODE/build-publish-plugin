package ru.kode.android.build.publish.plugin.sender.task.nextcloud

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import ru.kode.android.build.publish.plugin.sender.task.base.BaseNextcloudSenderTask
import ru.kode.android.build.publish.plugin.sender.task.nextcloud.work.UploadToNextcloudWork
import javax.inject.Inject

@DisableCachingByDefault
abstract class UploadToNextcloudTask
    @Inject
    constructor(
        workerExecutor: WorkerExecutor,
    ) : BaseNextcloudSenderTask(workerExecutor) {
        init {
            description = "Uploads a file to Nextcloud"
        }

        @get:InputFile
        @get:PathSensitive(PathSensitivity.RELATIVE)
        @get:Option(option = "file", description = "File to upload")
        abstract val file: RegularFileProperty

        @get:Input
        @get:Option(option = "remotePath", description = "Remote path on Nextcloud")
        abstract val remotePath: Property<String>

        @get:Input
        @get:Optional
        @get:Option(option = "remoteFileName", description = "Remote file name (defaults to local file name)")
        abstract val remoteFileName: Property<String>

        @TaskAction
        fun uploadFile() {
            workerExecutor.noIsolation().submit(UploadToNextcloudWork::class.java) { params ->
                params.baseUrl.set(baseUrl)
                params.username.set(username)
                params.password.set(password)
                params.file.set(file)
                params.remotePath.set(remotePath)
                params.remoteFileName.set(remoteFileName.orElse(file.asFile.map { it.name }))
            }
        }
    }
