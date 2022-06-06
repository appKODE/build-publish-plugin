package ru.kode.android.build.publish.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import ru.kode.android.build.publish.plugin.command.getCommandExecutor
import ru.kode.android.build.publish.plugin.git.GitRepository
import ru.kode.android.build.publish.plugin.git.mapper.toJson

abstract class GetLastTagTask : DefaultTask() {

    init {
        description = "Get last tag task"
        group = BasePlugin.BUILD_GROUP
    }

    private val commandExecutor = getCommandExecutor(project)

    @get:Input
    @get:Option(option = "buildVariant", description = "Current build variant")
    abstract val buildVariant: Property<String>

    @get:OutputFile
    @get:Option(option = "tagBuildFile", description = "Json contains info about tag build")
    abstract val tagBuildFile: RegularFileProperty

    @TaskAction
    fun getLastTag() {
        val variant = buildVariant.get()
        val buildTag = GitRepository(commandExecutor, setOf(variant)).findRecentBuildTag()
        val tagBuildOutput = tagBuildFile.get().asFile
        if (buildTag != null) {
            tagBuildOutput.writeText(buildTag.toJson())
        }
    }
}
