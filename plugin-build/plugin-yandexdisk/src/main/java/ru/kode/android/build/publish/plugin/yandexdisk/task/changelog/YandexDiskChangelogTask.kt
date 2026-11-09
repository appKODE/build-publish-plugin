package ru.kode.android.build.publish.plugin.yandexdisk.task.changelog

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import ru.kode.android.build.publish.plugin.yandexdisk.service.YandexDiskService
import ru.kode.android.build.publish.plugin.core.util.RemoteFileNameContext
import ru.kode.android.build.publish.plugin.yandexdisk.task.distribution.work.YandexDiskUploadWork
import ru.kode.android.build.publish.plugin.core.util.resolveChangelogRemoteFileName
import ru.kode.android.gradle.commons.logger.LoggerService
import javax.inject.Inject

@DisableCachingByDefault
abstract class YandexDiskChangelogTask
    @Inject
    constructor(
        private val workerExecutor: WorkerExecutor,
    ) : DefaultTask() {
        init {
            description = "Uploads generated changelog file to Yandex Disk"
            group = BasePlugin.BUILD_GROUP
        }

        @get:Internal
        abstract val service: Property<YandexDiskService>

        @get:Internal
        abstract val loggerService: Property<LoggerService>

        @get:InputFile
        @get:Option(
            option = "buildTagSnapshotFile",
            description = "Json contains info about tag build",
        )
        @get:PathSensitive(PathSensitivity.RELATIVE)
        abstract val buildTagSnapshotFile: RegularFileProperty

        @get:Input
        @get:Option(
            option = "baseOutputFileName",
            description = "Base file name for deterministic remote naming",
        )
        abstract val baseOutputFileName: Property<String>

        @get:Input
        @get:Option(
            option = "buildVariantName",
            description = "Current build variant name",
        )
        abstract val buildVariantName: Property<String>

        @get:Input
        @get:Optional
        @get:Option(
            option = "buildVariantDefaultVersionName",
            description = "Default Android versionName for current variant",
        )
        abstract val buildVariantDefaultVersionName: Property<String>

        @get:InputFile
        @get:Option(
            option = "changelogFile",
            description = "Generated changelog file (absolute path is expected)",
        )
        @get:PathSensitive(PathSensitivity.RELATIVE)
        abstract val changelogFile: RegularFileProperty

        @get:Input
        @get:Option(
            option = "remotePath",
            description = "Folder on Yandex Disk, relative to the disk root",
        )
        abstract val remotePath: Property<String>

        @get:Input
        @get:Optional
        @get:Option(
            option = "publicLink",
            description = "Whether to publish the uploaded file and log its public link",
        )
        abstract val publicLink: Property<Boolean>

        @get:Input
        @get:Optional
        @get:Option(
            option = "remoteFileName",
            description = "Explicit remote file name override",
        )
        abstract val remoteFileName: Property<String>

        @TaskAction
        fun upload() {
            val changelog = changelogFile.asFile.get()
            val remoteNameContext =
                RemoteFileNameContext(
                    baseOutputFileName = baseOutputFileName.get(),
                    buildVariantName = buildVariantName.get(),
                    buildVariantDefaultVersionName = buildVariantDefaultVersionName.orNull,
                    buildTagSnapshotFile = buildTagSnapshotFile.asFile.get(),
                )
            val resolvedRemoteFileName =
                resolveChangelogRemoteFileName(
                    context = remoteNameContext,
                    sourceFile = changelog,
                    explicitRemoteFileName = remoteFileName.orNull,
                )

            val workQueue: WorkQueue = workerExecutor.noIsolation()
            workQueue.submit(YandexDiskUploadWork::class.java) { parameters ->
                parameters.outputFile.set(changelogFile)
                parameters.remotePath.set(remotePath)
                parameters.service.set(service)
                parameters.loggerService.set(loggerService)
                parameters.compressed.set(false)
                parameters.remoteFileName.set(resolvedRemoteFileName)
                parameters.publicLink.set(publicLink.orElse(false))
            }
        }
    }
