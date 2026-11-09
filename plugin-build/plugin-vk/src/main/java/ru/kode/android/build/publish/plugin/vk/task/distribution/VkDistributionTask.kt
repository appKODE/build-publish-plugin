package ru.kode.android.build.publish.plugin.vk.task.distribution

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
import ru.kode.android.build.publish.plugin.core.util.RemoteFileNameContext
import ru.kode.android.build.publish.plugin.core.util.resolveArtifactRemoteFileName
import ru.kode.android.build.publish.plugin.vk.service.VkService
import ru.kode.android.build.publish.plugin.vk.task.distribution.work.VkUploadWork
import ru.kode.android.gradle.commons.logger.LoggerService
import javax.inject.Inject

@DisableCachingByDefault
abstract class VkDistributionTask
    @Inject
    constructor(
        private val workerExecutor: WorkerExecutor,
    ) : DefaultTask() {
        init {
            description = "Sends distribution files to a VK conversation"
            group = BasePlugin.BUILD_GROUP
        }

        @get:Internal
        abstract val service: Property<VkService>

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
            description = "Base file name for deterministic document naming",
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
            option = "distributionFile",
            description = "Artifact output file (absolute path is expected)",
        )
        @get:PathSensitive(PathSensitivity.RELATIVE)
        abstract val distributionFile: RegularFileProperty

        @get:Input
        @get:Optional
        @get:Option(
            option = "compressed",
            description = "Whether to compress the distribution file before sending",
        )
        internal abstract val compressed: Property<Boolean>

        @get:Input
        @get:Option(
            option = "peerId",
            description = "VK conversation id: user id or 2000000000 + chat_id",
        )
        abstract val peerId: Property<Long>

        @get:Input
        @get:Optional
        @get:Option(
            option = "message",
            description = "Optional text sent with the document",
        )
        abstract val message: Property<String>

        @get:Input
        @get:Optional
        @get:Option(
            option = "title",
            description = "Explicit document title override",
        )
        abstract val title: Property<String>

        @TaskAction
        fun upload() {
            val outputFile = distributionFile.asFile.get()
            val compressed = compressed.orElse(false).get()
            val remoteNameContext =
                RemoteFileNameContext(
                    baseOutputFileName = baseOutputFileName.get(),
                    buildVariantName = buildVariantName.get(),
                    buildVariantDefaultVersionName = buildVariantDefaultVersionName.orNull,
                    buildTagSnapshotFile = buildTagSnapshotFile.asFile.get(),
                )
            val resolvedTitle =
                resolveArtifactRemoteFileName(
                    context = remoteNameContext,
                    sourceFile = outputFile,
                    compressed = compressed,
                    explicitRemoteFileName = title.orNull,
                )

            val workQueue: WorkQueue = workerExecutor.noIsolation()
            workQueue.submit(VkUploadWork::class.java) { parameters ->
                parameters.outputFile.set(distributionFile)
                parameters.peerId.set(peerId)
                parameters.title.set(resolvedTitle)
                parameters.message.set(message)
                parameters.compressed.set(compressed)
                parameters.service.set(service)
                parameters.loggerService.set(loggerService)
            }
        }
    }
