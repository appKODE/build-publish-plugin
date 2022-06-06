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
import java.net.URLEncoder

abstract class SendTelegramChangelogTask : DefaultTask() {

    init {
        description = "Task to send changelog for Telegram"
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
    @get:Option(option = "botId", description = "Bot id where webhook posted")
    abstract val botId: Property<String>

    @get:Input
    @get:Option(option = "chatId", description = "Chat id where webhook posted")
    abstract val chatId: Property<String>

    @get:Input
    @get:Option(option = "userMentions", description = "User tags to mention in chat")
    abstract val userMentions: SetProperty<String>

    @TaskAction
    fun sendChangelog() {
        val currentBuildTagName = fromJson(tagBuildFile.asFile.get()).name
        val escapedCharacters =
            "[_]|[*]|[\\[]|[\\]]|[(]|[)]|[~]|[`]|[>]|[#]|[+]|[=]|[|]|[{]|[}]|[.]|[!]".toRegex()
        val changelog = changelogFile.orNull?.asFile?.readText()
        if (changelog.isNullOrEmpty()) {
            project.logger.error(
                "[sendChangelog] changelog file not found, is empty or error occurred"
            )
        } else {
            val telegramFormattedChangelog = changelog.formatChangelogToTelegram(escapedCharacters)
            if (telegramFormattedChangelog.isNotBlank()) {
                sendTelegramChangelog(telegramFormattedChangelog, currentBuildTagName, escapedCharacters)
            } else {
                project.logger.error(
                    "[sendChangelog] changelog not sent to telegram, is empty or error occurred"
                )
            }
        }
    }

    private fun sendTelegramChangelog(
        changelog: String,
        currentBuildTagName: String,
        escapedCharacters: Regex
    ) {
        val baseOutputFileName = baseOutputFileName.get()
        val tgUserMentions = userMentions.orNull.orEmpty().joinToString(", ")
        val title = (
            "$baseOutputFileName $currentBuildTagName\n" +
                "${tgUserMentions}\n\n"
            )
            .replace(escapedCharacters) { result -> "\\${result.value}" }
            .replace("[-]".toRegex()) { result -> "\\${result.value}" }

        val url = webhookUrl.get().format(
            botId.get(),
            chatId.get(),
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
}
