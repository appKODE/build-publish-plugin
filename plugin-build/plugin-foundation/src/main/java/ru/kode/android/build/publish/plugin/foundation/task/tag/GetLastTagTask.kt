package ru.kode.android.build.publish.plugin.foundation.task.tag

import org.ajoberstar.grgit.gradle.GrgitService
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import ru.kode.android.build.publish.plugin.foundation.task.tag.work.GenerateTagWork
import javax.inject.Inject

@DisableCachingByDefault
abstract class GetLastTagTask
    @Inject
    constructor(
        private val workerExecutor: WorkerExecutor,
        objectFactory: ObjectFactory,
    ) : DefaultTask() {
        private var grgitService: Property<GrgitService>

        init {
            description = "Get last tag task"
            group = BasePlugin.BUILD_GROUP
            grgitService = objectFactory.property(GrgitService::class.java)
        }

        @Internal
        fun getGrgitService(): Property<GrgitService> = grgitService

        @get:Input
        @get:Option(option = "buildVariant", description = "Current build variant")
        abstract val buildVariant: Property<String>

        @get:Input
        @get:Option(option = "buildTagPattern", description = "Tag pattern to correctly search related tags")
        @get:Optional
        abstract val buildTagPattern: Property<String>

        @get:OutputFile
        @get:Option(option = "tagBuildFile", description = "Json contains info about tag build")
        abstract val tagBuildFile: RegularFileProperty

        @TaskAction
        fun getLastTag() {
            val workQueue: WorkQueue = workerExecutor.noIsolation()
            workQueue.submit(GenerateTagWork::class.java) { parameters ->
                parameters.tagBuildFile.set(tagBuildFile)
                parameters.buildVariant.set(buildVariant)
                parameters.buildTagPattern.set(buildTagPattern)
                parameters.grgitService.set(grgitService)
            }
            workQueue.await()
        }
    }
