package ru.kode.android.build.publish.plugin.foundation.task.tag

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import ru.kode.android.build.publish.plugin.foundation.service.git.GitExecutorService
import ru.kode.android.build.publish.plugin.foundation.task.tag.work.GenerateTagWork
import javax.inject.Inject

@DisableCachingByDefault
abstract class GetLastTagTask
    @Inject
    constructor(
        private val workerExecutor: WorkerExecutor,
    ) : DefaultTask() {
        init {
            description = "Get last tag task"
            group = BasePlugin.BUILD_GROUP
            outputs.upToDateWhen { false }
        }

        @get:ServiceReference
        abstract val gitExecutorService: Property<GitExecutorService>

        @get:Input
        @get:Option(option = "buildVariantName", description = "Current build variant")
        abstract val buildVariantName: Property<String>

        @get:Input
        @get:Option(option = "buildTagPattern", description = "Tag pattern to correctly search related tags")
        abstract val buildTagPattern: Property<String>

        @get:Input
        @get:Option(
            option = "useStubsForTagAsFallback",
            description = "Use stubs if tag was not found to not crash builds",
        )
        abstract val useStubsForTagAsFallback: Property<Boolean>

        @get:OutputFile
        @get:Option(option = "tagBuildFile", description = "Json contains info about tag build")
        abstract val tagBuildFile: RegularFileProperty

        @TaskAction
        fun getLastTag() {
            val workQueue: WorkQueue = workerExecutor.noIsolation()
            workQueue.submit(GenerateTagWork::class.java) { parameters ->
                parameters.tagBuildFile.set(tagBuildFile)
                parameters.buildVariant.set(buildVariantName)
                parameters.buildTagPattern.set(buildTagPattern)
                parameters.gitExecutorService.set(gitExecutorService)
                parameters.useStubsForTagAsFallback.set(useStubsForTagAsFallback)
            }
            workQueue.await()
        }
    }
