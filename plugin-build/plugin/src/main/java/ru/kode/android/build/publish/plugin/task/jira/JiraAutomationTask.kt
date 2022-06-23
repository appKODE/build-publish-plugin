package ru.kode.android.build.publish.plugin.task.jira

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import ru.kode.android.build.publish.plugin.enity.mapper.fromJson
import ru.kode.android.build.publish.plugin.task.jira.work.JiraAutomationWork
import javax.inject.Inject

abstract class JiraAutomationTask @Inject constructor(
    private val workerExecutor: WorkerExecutor,
) : DefaultTask() {

    init {
        description = "Task to send apk to Slack"
        group = BasePlugin.BUILD_GROUP
    }

    @get:InputFile
    @get:Option(option = "tagBuildFile", description = "Json contains info about tag build")
    abstract val tagBuildFile: RegularFileProperty

    @get:InputFile
    @get:Option(option = "changelogFile", description = "File with saved changelog")
    abstract val changelogFile: RegularFileProperty

    @get:Input
    @get:Option(
        option = "issueNumberPattern",
        description = "How task number formatted"
    )
    abstract val issueNumberPattern: Property<String>

    @get:Input
    @get:Option(
        option = "baseUrl",
        description = " Api token file to upload files in slack"
    )
    abstract val baseUrl: Property<String>

    @get:Input
    @get:Option(
        option = "username",
        description = " Api token file to upload files in slack"
    )
    abstract val username: Property<String>

    @get:Input
    @get:Option(
        option = "password",
        description = " Api token file to upload files in slack"
    )
    abstract val password: Property<String>

    @get:Input
    @get:Option(
        option = "labelPattern",
        description = " Api token file to upload files in slack"
    )
    abstract val labelPattern: Property<String>

    @TaskAction
    fun upload() {
        val currentBuildTag = fromJson(tagBuildFile.asFile.get())
        val label = labelPattern.get()
            .format(
                currentBuildTag.buildNumber,
                currentBuildTag.buildVariant,
                currentBuildTag.name
            )
        val changelog = changelogFile.asFile.get().readText()
        val issues = Regex(issueNumberPattern.get())
            .findAll(changelog)
            .mapTo(mutableSetOf()) { it.groupValues[0] }
        val workQueue: WorkQueue = workerExecutor.noIsolation()
        if (issues.isEmpty()) {
            logger.warn("issues not found in the changelog, nothing will change")
        } else {
            workQueue.submit(JiraAutomationWork::class.java) { parameters ->
                parameters.baseUrl.set(baseUrl)
                parameters.username.set(username)
                parameters.password.set(password)
                parameters.issues.set(issues)
                parameters.label.set(label)
            }
        }
    }
}
