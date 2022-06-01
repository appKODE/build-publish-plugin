package ru.kode.android.build.publish.plugin.task

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import ru.kode.android.build.publish.plugin.git.mapper.fromJson

abstract class PrintLastIncreasedTag : DefaultTask() {

    init {
        description = "Task to print last increased tag"
        group = BasePlugin.BUILD_GROUP
    }

    @get:InputFile
    @get:Option(option = "tagBuildFile", description = "Json contains info about tag build")
    abstract val tagBuildFile: RegularFileProperty

    @TaskAction
    fun printTag() {
        val tagBuildFile = tagBuildFile.asFile.get()
        val currentBuildTag = fromJson(tagBuildFile)
        val currentBuildNumber = currentBuildTag.buildNumber.toString()
        val increasedBuildNumber = currentBuildTag.buildNumber.inc().toString()
        val nextTag = currentBuildTag.name.replaceFirst(currentBuildNumber, increasedBuildNumber)
        println(nextTag)
    }

    override fun doLast(action: Action<in Task>): Task {
        return super.doLast(action)
    }
}
