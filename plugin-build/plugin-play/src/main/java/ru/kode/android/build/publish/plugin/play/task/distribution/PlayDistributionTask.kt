package ru.kode.android.build.publish.plugin.play.task.distribution

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import ru.kode.android.build.publish.plugin.core.mapper.fromJson
import ru.kode.android.build.publish.plugin.play.service.PlayNetworkService
import ru.kode.android.build.publish.plugin.play.task.distribution.work.PlayUploadWork
import javax.inject.Inject

/**
 * Task to publish app at given release track in google play console with set priority
 * Contains basic functionality, for the extensions reference to original implementation:
 * https://github.com/Triple-T/gradle-play-publisher
 */
abstract class PlayDistributionTask
    @Inject
    constructor(
        private val workerExecutor: WorkerExecutor,
    ) : DefaultTask() {

        init {
            description = "Task to send apk to Google Play"
            group = BasePlugin.BUILD_GROUP
        }

        @get:Internal
        abstract val networkService: Property<PlayNetworkService>

        @get:InputFile
        @get:Option(
            option = "buildVariantOutputFile",
            description = "Artifact output file (absolute path is expected)",
        )
        abstract val buildVariantOutputFile: RegularFileProperty

        @get:InputFile
        @get:Option(option = "tagBuildFile", description = "Json contains info about tag build")
        abstract val tagBuildFile: RegularFileProperty


        @get:Input
        @get:Option(
            option = "trackId",
            description = "Track name of target app. Defaults to internal",
        )
        abstract val trackId: Property<String>

        @get:Input
        @get:Optional
        @get:Option(
            option = "updatePriority",
            description = "Update priority (0..5)",
        )
        abstract val updatePriority: Property<Int>

        @TaskAction
        fun upload() {
            val outputFile = buildVariantOutputFile.asFile.get()
            if (outputFile.extension != "aab") throw GradleException("file ${outputFile.path} is not bundle, not possible to deploy it to Google Play")
            val tag = fromJson(tagBuildFile.asFile.get())
            val releaseName = "${tag.name}(${tag.buildVersion}.${tag.buildNumber})"
            val trackId = trackId.orNull ?: "internal"
            val updatePriority = updatePriority.orNull ?: 0

            val workQueue: WorkQueue = workerExecutor.noIsolation()
            workQueue.submit(PlayUploadWork::class.java) { parameters ->
                parameters.trackId.set(trackId)
                parameters.updatePriority.set(updatePriority)
                parameters.releaseName.set(releaseName)
                parameters.outputFile.set(outputFile)
                parameters.networkService.set(networkService)
            }
        }
    }
