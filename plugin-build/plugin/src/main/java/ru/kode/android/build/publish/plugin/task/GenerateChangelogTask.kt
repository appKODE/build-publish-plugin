package ru.kode.android.build.publish.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import ru.kode.android.build.publish.plugin.task.work.GenerateChangelogWork
import javax.inject.Inject

/**
 * Generate changelog task should write to file,
 * then result can be used in Firebase App Distribution without rebuilding configs
 * and it can be used in all other tasks without duplicates in different places
 */
abstract class GenerateChangelogTask @Inject constructor(
    private val workerExecutor: WorkerExecutor,
) : DefaultTask() {

    init {
        description = "Task to generate changelog"
        group = BasePlugin.BUILD_GROUP
    }

    @get:InputFile
    @get:Option(option = "tagBuildFile", description = "Json contains info about tag build")
    abstract val tagBuildFile: RegularFileProperty

    @get:Input
    @get:Option(option = "buildVariant", description = "Current build variant")
    abstract val buildVariant: Property<String>

    @get:Input
    @get:Option(
        option = "commitMessageKey",
        description = "Message key to collect interested commits"
    )
    abstract val commitMessageKey: Property<String>

    @get:OutputFile
    @get:Option(
        option = "changelogFile",
        description = "File with saved changelog"
    )
    abstract val changelogFile: RegularFileProperty

    @TaskAction
    fun generateChangelog() {
        val workQueue: WorkQueue = workerExecutor.noIsolation()
        workQueue.submit(GenerateChangelogWork::class.java) { parameters ->
            parameters.commitMessageKey.set(commitMessageKey)
            parameters.buildVariant.set(buildVariant)
            parameters.tagBuildFile.set(tagBuildFile)
            parameters.changelogFile.set(changelogFile)
        }
    }
}
