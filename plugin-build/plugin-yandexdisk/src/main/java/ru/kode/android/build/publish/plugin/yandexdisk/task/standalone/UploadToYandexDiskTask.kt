package ru.kode.android.build.publish.plugin.yandexdisk.task.standalone

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
import ru.kode.android.build.publish.plugin.core.task.StandaloneServiceTask
import ru.kode.android.build.publish.plugin.yandexdisk.service.YandexDiskService
import ru.kode.android.build.publish.plugin.yandexdisk.task.standalone.work.UploadToYandexDiskWork
import javax.inject.Inject

@DisableCachingByDefault
abstract class UploadToYandexDiskTask
    @Inject
    constructor(
        workerExecutor: WorkerExecutor,
    ) : StandaloneServiceTask<YandexDiskService>(workerExecutor) {
        init {
            description = "Uploads a file to Yandex Disk"
        }

        @get:InputFile
        @get:Option(option = "file", description = "File to upload (absolute path)")
        @get:PathSensitive(PathSensitivity.RELATIVE)
        abstract val file: RegularFileProperty

        @get:Input
        @get:Option(option = "remotePath", description = "Remote folder path on Yandex Disk")
        abstract val remotePath: Property<String>

        @get:Input
        @get:Optional
        @get:Option(option = "remoteFileName", description = "Remote file name (defaults to local file name)")
        abstract val remoteFileName: Property<String>

        @TaskAction
        fun upload() {
            val resolvedFileName =
                if (remoteFileName.isPresent) {
                    remoteFileName.get()
                } else {
                    file.asFile.get().name
                }
            val workQueue = workerExecutor.noIsolation()
            workQueue.submit(UploadToYandexDiskWork::class.java) { params ->
                params.file.set(file)
                params.remotePath.set(remotePath)
                params.remoteFileName.set(resolvedFileName)
                params.service.set(service)
                params.loggerService.set(loggerService)
            }
        }
    }
