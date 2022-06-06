package ru.kode.android.build.publish.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import ru.kode.android.build.publish.plugin.command.getCommandExecutor
import ru.kode.android.build.publish.plugin.git.mapper.fromJson

abstract class SendSlackChangelogTask : DefaultTask() {

    init {
        description = "Task to send changelog for Slack"
        group = BasePlugin.BUILD_GROUP
    }

    private val commandExecutor = getCommandExecutor(project)

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
        val currentBuildTagName = fromJson(tagBuildFile.asFile.get()).name
        val changelog = changelogFile.orNull?.asFile?.readText()
        if (changelog.isNullOrEmpty()) {
            project.logger.error(
                "[sendChangelog] changelog file not found, is empty or error occurred"
            )
        } else {
            val slackFormattedChangelog = changelog.formatChangelogToSlack()
            if (slackFormattedChangelog.isNotBlank()) {
                sendSlackChangelog(currentBuildTagName, slackFormattedChangelog)
            } else {
                project.logger.error(
                    "[sendChangelog] changelog not sent to Slack, is empty or error occurred"
                )
            }
        }
    }

    private fun sendSlackChangelog(
        currentBuildTagName: String,
        changelog: String
    ) {
        val baseOutputFileName = baseOutputFileName.get()
        val slackUserMentions = userMentions.orNull.orEmpty().joinToString(", ")
        val json = "\"{" +
            "\\\"icon_url\\\": \\\"${iconUrl}\\\"," +
            "\\\"username\\\": \\\"buildBot\\\", " +
            "\\\"blocks\\\": [ " +
            "{ \\\"type\\\": \\\"section\\\", \\\"text\\\": { " +
            "\\\"type\\\": \\\"mrkdwn\\\", " +
            "\\\"text\\\": \\\"*$baseOutputFileName $currentBuildTagName*" +
            " $slackUserMentions\\n\\n$changelog\\\" " +
            "}" +
            "} ]" +
            "}\""
        commandExecutor.sendToWebHook(webhookUrl.get(), json)
        project.logger.debug("changelog sent to Slack")
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
