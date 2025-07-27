package ru.kode.android.build.publish.plugin.task.tag

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import ru.kode.android.build.publish.plugin.enity.mapper.fromJson

@DisableCachingByDefault
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
        val lastBuildTag = fromJson(tagBuildFile.asFile.get())
        val currentBuildNumber = lastBuildTag.buildNumber.toString()
        val increasedBuildNumber = lastBuildTag.buildNumber.inc().toString()
        val nextBuildTag = lastBuildTag.name.replaceFirst(currentBuildNumber, increasedBuildNumber)
        print(nextBuildTag)
    }
}
