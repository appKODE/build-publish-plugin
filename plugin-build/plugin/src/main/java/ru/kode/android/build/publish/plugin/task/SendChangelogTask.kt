package ru.kode.android.build.publish.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import ru.kode.android.build.publish.plugin.command.getCommandExecutor
import ru.kode.android.build.publish.plugin.error.ValueNotFoundException
import ru.kode.android.build.publish.plugin.git.mapper.fromJson
import java.net.URLEncoder

private data class TelegramSendConfig(
    val webhookUrl: String,
    val botId: String,
    val chatId: String
) {
    constructor(values: Map<String, String>) : this(
        webhookUrl = values["webhook_url"] ?: throw ValueNotFoundException("webhook_url"),
        botId = values["bot_id"] ?: throw ValueNotFoundException("bot_id"),
        chatId = values["chat_id"] ?: throw ValueNotFoundException("chat_id")
    )
}

private data class SlackSendConfig(
    val webhookUrl: String,
    val iconUrl: String,
) {
    constructor(values: Map<String, String>) : this(
        webhookUrl = values["webhook_url"] ?: throw ValueNotFoundException("webhook_url"),
        iconUrl = values["icon_url"] ?: throw ValueNotFoundException("icon_url"),
    )
}

abstract class SendChangelogTask : DefaultTask() {

    init {
        description = "Task to send changelog"
        group = BasePlugin.BUILD_GROUP
    }

    private val commandExecutor = getCommandExecutor(project)

    @get:InputFile
    @get:Option(option = "changelogFIle", description = "File with saved changelog")
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
    @get:Option(option = "tgConfig", description = "Config for Telegram changelog sender")
    abstract val tgConfig: MapProperty<String, String>

    @get:Input
    @get:Option(option = "tgUserMentions", description = "List of mentioning users for tg")
    abstract val tgUserMentions: SetProperty<String>

    @get:Input
    @get:Option(option = "slackConfig", description = "Config for Slack changelog sender")
    abstract val slackConfig: MapProperty<String, String>

    @get:Input
    @get:Option(option = "slackUserMentions", description = "List of mentioning users for Slack")
    abstract val slackUserMentions: SetProperty<String>

    @TaskAction
    fun sendChangelog() {
        val tagBuildFile = tagBuildFile.asFile.get()
        val currentBuildTagName = fromJson(tagBuildFile).name
        val escapedCharacters =
            "[_]|[*]|[\\[]|[\\]]|[(]|[)]|[~]|[`]|[>]|[#]|[+]|[=]|[|]|[{]|[}]|[.]|[!]".toRegex()
        val changelog = changelogFile.orNull?.asFile?.readText()
        if (changelog.isNullOrEmpty()) {
            project.logger.error(
                "[sendChangelog] changelog file not found, is empty or error occurred"
            )
        } else {
            val telegramFormattedChangelog = changelog.formatChangelogToTelegram(escapedCharacters)
            if (!tgConfig.orNull.isNullOrEmpty() && telegramFormattedChangelog.isNotBlank()) {
                sendTelegramChangelog(telegramFormattedChangelog, currentBuildTagName, escapedCharacters)
            } else {
                project.logger.error(
                    "[sendChangelog] changelog not sent to telegram, is empty or error occurred"
                )
            }
            val slackFormattedChangelog = changelog.formatChangelogToSlack()
            if (!slackConfig.orNull.isNullOrEmpty() && slackFormattedChangelog.isNotBlank()) {
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
        val slackConfig = SlackSendConfig(slackConfig.get())
        val slackUserMentions = slackUserMentions.orNull.orEmpty().joinToString(", ")
        val json = "\"{" +
            "\\\"icon_url\\\": \\\"${slackConfig.iconUrl}\\\"," +
            "\\\"username\\\": \\\"buildBot\\\", " +
            "\\\"blocks\\\": [ " +
            "{ \\\"type\\\": \\\"section\\\", \\\"text\\\": { " +
            "\\\"type\\\": \\\"mrkdwn\\\", " +
            "\\\"text\\\": \\\"*$baseOutputFileName $currentBuildTagName*" +
            " $slackUserMentions\\n\\n$changelog\\\" " +
            "}" +
            "} ]" +
            "}\""
        commandExecutor.sendToWebHook(slackConfig.webhookUrl, json)
        project.logger.debug("changelog sent to Slack")
    }

    private fun sendTelegramChangelog(
        changelog: String,
        currentBuildTagName: String,
        escapedCharacters: Regex
    ) {
        val baseOutputFileName = baseOutputFileName.get()
        val tgConfig = TelegramSendConfig(tgConfig.get())
        val tgUserMentions = tgUserMentions.orNull.orEmpty().joinToString(", ")
        val title = (
            "$baseOutputFileName $currentBuildTagName\n" +
                "${tgUserMentions}\n\n"
            )
            .replace(escapedCharacters) { result -> "\\${result.value}" }
            .replace("[-]".toRegex()) { result -> "\\${result.value}" }

        val url = tgConfig.webhookUrl.format(
            tgConfig.botId,
            tgConfig.chatId,
            URLEncoder.encode("$title$changelog", "utf-8")
        )
        commandExecutor.sendToWebHook(url)
        project.logger.debug("changelog sent to Telegram")
    }

    private fun String.formatChangelogToTelegram(escapedCharacters: Regex): String {
        val issueUrlPrefix = issueUrlPrefix.get()
        val issueNumberPattern = issueNumberPattern.get()
        val issueRegexp = Regex(issueNumberPattern)
        return this
            .replace(escapedCharacters) { result -> "\\${result.value}" }
            .replace(issueRegexp) { result -> "[${result.value}](${issueUrlPrefix}${result.value})" }
            .replace(Regex("(\r\n|\n)"), "\n")
            .replace("[-]".toRegex()) { result -> "\\${result.value}" }
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
