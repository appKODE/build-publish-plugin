package ru.kode.android.build.publish.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import ru.kode.android.build.publish.plugin.task.work.GenerateTagWork
import javax.inject.Inject

abstract class GetLastTagTask @Inject constructor(
    private val workerExecutor: WorkerExecutor,
) : DefaultTask() {

    init {
        description = "Get last tag task"
        group = BasePlugin.BUILD_GROUP
    }

    @get:Input
    @get:Option(option = "buildVariant", description = "Current build variant")
    abstract val buildVariant: Property<String>

    @get:OutputFile
    @get:Option(option = "tagBuildFile", description = "Json contains info about tag build")
    abstract val tagBuildFile: RegularFileProperty

    @TaskAction
    fun getLastTag() {
        val workQueue: WorkQueue = workerExecutor.noIsolation()
        workQueue.submit(GenerateTagWork::class.java) { parameters ->
            parameters.tagBuildFile.set(tagBuildFile)
            parameters.buildVariant.set(buildVariant)
        }
    }
}
