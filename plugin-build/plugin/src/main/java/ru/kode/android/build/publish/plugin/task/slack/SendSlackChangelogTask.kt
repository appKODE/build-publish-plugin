package ru.kode.android.build.publish.plugin.task.slack

import org.gradle.api.DefaultTask
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
import ru.kode.android.build.publish.plugin.enity.mapper.fromJson
import ru.kode.android.build.publish.plugin.task.slack.work.SendSlackChangelogWork
import javax.inject.Inject

abstract class SendSlackChangelogTask @Inject constructor(
    private val workerExecutor: WorkerExecutor,
) : DefaultTask() {

    init {
        description = "Task to send changelog for Slack"
        group = BasePlugin.BUILD_GROUP
    }

    @get:InputFile
    @get:Option(option = "changelogFile", description = "File with saved changelog")
    abstract val changelogFile: RegularFileProperty

    @get:InputFile
    @get:Option(option = "tagBuildFile", description = "Json contains info about tag build")
    abstract val tagBuildFile: RegularFileProperty

    @get:Input
    @get:Option(
        option = "baseOutputFileName",
        description = "Application bundle name for changelog"
    )
    abstract val baseOutputFileName: Property<String>

    @get:Input
    @get:Option(
        option = "issueUrlPrefix",
        description = "Address of task tracker"
    )
    abstract val issueUrlPrefix: Property<String>

    @get:Input
    @get:Option(
        option = "issueNumberPattern",
        description = "How task number formatted"
    )
    abstract val issueNumberPattern: Property<String>

    @get:Input
    @get:Option(option = "webhookUrl", description = "Webhook url to send changelog")
    abstract val webhookUrl: Property<String>

    @get:Input
    @get:Option(option = "iconUrl", description = "Icon url to show in chat")
    abstract val iconUrl: Property<String>

    @get:Input
    @get:Option(option = "userMentions", description = "List of mentioning users for Slack")
    abstract val userMentions: SetProperty<String>

    @TaskAction
    fun sendChangelog() {
        val currentBuildTag = fromJson(tagBuildFile.asFile.get())
        val changelog = changelogFile.orNull?.asFile?.readText()
        if (changelog.isNullOrEmpty()) {
            logger.error(
                "changelog file not found, is empty or error occurred"
            )
        } else {
            val slackFormattedChangelog = changelog.formatChangelogToSlack()
            if (slackFormattedChangelog.isNotBlank()) {
                val slackUserMentions = userMentions.orNull.orEmpty().joinToString(", ")
                val workQueue: WorkQueue = workerExecutor.noIsolation()
                workQueue.submit(SendSlackChangelogWork::class.java) { parameters ->
                    parameters.baseOutputFileName.set(baseOutputFileName)
                    parameters.webhookUrl.set(webhookUrl)
                    parameters.iconUrl.set(iconUrl)
                    parameters.buildName.set(currentBuildTag.name)
                    parameters.changelog.set(changelog)
                    parameters.userMentions.set(slackUserMentions)
                }
            } else {
                logger.error(
                    "changelog not sent to Slack, is empty or error occurred"
                )
            }
        }
    }

    private fun String.formatChangelogToSlack(): String {
        val issueUrlPrefix = issueUrlPrefix.get()
        val issueNumberPattern = issueNumberPattern.get()
        return this
            .replace(Regex(issueNumberPattern), "<$issueUrlPrefix\$0|\$0>")
            .replace(Regex("(\r\n|\n)"), "\\\\n")
            // only this insane amount of quotes works! they are needed to produce \\\" in json
            .replace(Regex("\""), "\\\\\\\\\\\\\"")
    }
}
