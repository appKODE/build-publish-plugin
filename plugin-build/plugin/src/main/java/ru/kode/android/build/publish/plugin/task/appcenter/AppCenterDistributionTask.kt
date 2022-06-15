package ru.kode.android.build.publish.plugin.task.appcenter

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import ru.kode.android.build.publish.plugin.git.mapper.fromJson
import ru.kode.android.build.publish.plugin.task.appcenter.work.AppCenterUploadWork
import javax.inject.Inject

abstract class AppCenterDistributionTask @Inject constructor(
    private val workerExecutor: WorkerExecutor,
) : DefaultTask() {

    init {
        description = "Task to send apk to AppCenter"
        group = BasePlugin.BUILD_GROUP
    }

    @get:InputFile
    @get:Option(
        option = "changelogFile",
        description = "File with saved changelog"
    )
    abstract val changelogFile: RegularFileProperty

    @get:InputFile
    @get:Option(
        option = "buildVariantOutputFile",
        description = "Artifact output file (absolute path is expected)"
    )
    abstract val buildVariantOutputFile: RegularFileProperty

    @get:InputFile
    @get:Option(option = "tagBuildFile", description = "Json contains info about tag build")
    abstract val tagBuildFile: RegularFileProperty

    @get:InputFile
    @get:Option(
        option = "apiTokenFile",
        description = "API token for target project in AppCenter"
    )
    abstract val apiTokenFile: RegularFileProperty

    @get:Input
    @get:Option(
        option = "ownerName",
        description = "Owner name of target project in AppCenter"
    )
    abstract val ownerName: Property<String>

    @get:Input
    @get:Option(
        option = "appName",
        description = "Application prefix for application name in AppCenter"
    )
    abstract val appNamePrefix: Property<String>

    @get:Input
    @get:Option(option = "testerGroups", description = "Distribution group names")
    abstract val testerGroups: SetProperty<String>

    @TaskAction
    fun upload() {
        val outputFile = buildVariantOutputFile.asFile.get()
        if (outputFile.extension != "apk") throw GradleException("file ${outputFile.path} is not apk")
        val currentBuildTag = fromJson(tagBuildFile.asFile.get())
        val changelogFile = changelogFile.asFile.get()
        val apiTokenFile = apiTokenFile.asFile.get()
        val workQueue: WorkQueue = workerExecutor.noIsolation()
        workQueue.submit(AppCenterUploadWork::class.java) { parameters ->
            parameters.ownerName.set(ownerName)
            parameters.appName.set(appNamePrefix.map { "$it-${currentBuildTag.buildVariant}" })
            parameters.buildName.set(currentBuildTag.name)
            parameters.buildNumber.set(currentBuildTag.buildNumber.toString())
            parameters.apiToken.set(apiTokenFile.readText())
            parameters.outputFile.set(outputFile)
            parameters.testerGroups.set(testerGroups)
            parameters.changelogFile.set(changelogFile)
        }
    }
}
