package ru.kode.android.build.publish.plugin.nextcloud.task.changelog

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
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
import ru.kode.android.build.publish.plugin.core.logger.LoggerService
import ru.kode.android.build.publish.plugin.nextcloud.config.NextcloudShareMode
import ru.kode.android.build.publish.plugin.nextcloud.service.NextcloudService
import ru.kode.android.build.publish.plugin.nextcloud.task.NextcloudRemoteFileNameContext
import ru.kode.android.build.publish.plugin.nextcloud.task.distribution.work.NextcloudUploadWork
import ru.kode.android.build.publish.plugin.nextcloud.task.resolveChangelogRemoteFileName
import javax.inject.Inject

@DisableCachingByDefault
abstract class NextcloudChangelogTask
    @Inject
    constructor(
        private val workerExecutor: WorkerExecutor,
    ) : DefaultTask() {
        init {
            description = "Uploads generated changelog file to Nextcloud"
            group = BasePlugin.BUILD_GROUP
            shareMode.convention(NextcloudShareMode.INTERNAL_RECIPIENTS)
            userRecipients.convention(emptySet())
            groupRecipients.convention(emptySet())
        }

        @get:Internal
        abstract val service: Property<NextcloudService>

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
            description = "Remote Nextcloud folder path under /remote.php/dav/files/{user}/",
        )
        abstract val remotePath: Property<String>

        @get:Input
        @get:Optional
        @get:Option(
            option = "shareMode",
            description = "Nextcloud share mode: INTERNAL_RECIPIENTS or PUBLIC_LINK",
        )
        abstract val shareMode: Property<NextcloudShareMode>

        @get:Input
        @get:Optional
        @get:Option(
            option = "userRecipients",
            description = "User recipients for internal sharing",
        )
        abstract val userRecipients: SetProperty<String>

        @get:Input
        @get:Optional
        @get:Option(
            option = "groupRecipients",
            description = "Group recipients for internal sharing",
        )
        abstract val groupRecipients: SetProperty<String>

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
                NextcloudRemoteFileNameContext(
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
            workQueue.submit(NextcloudUploadWork::class.java) { parameters ->
                parameters.outputFile.set(changelogFile)
                parameters.remotePath.set(remotePath)
                parameters.service.set(service)
                parameters.loggerService.set(loggerService)
                parameters.compressed.set(false)
                parameters.remoteFileName.set(resolvedRemoteFileName)
                parameters.shareMode.set(shareMode.orElse(NextcloudShareMode.INTERNAL_RECIPIENTS))
                parameters.userRecipients.set(userRecipients)
                parameters.groupRecipients.set(groupRecipients)
            }
        }
    }
