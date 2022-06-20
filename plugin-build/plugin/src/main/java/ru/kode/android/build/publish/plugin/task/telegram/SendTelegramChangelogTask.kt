package ru.kode.android.build.publish.plugin.task.telegram

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
import ru.kode.android.build.publish.plugin.task.telegram.work.SendTelegramChangelogWork
import javax.inject.Inject

abstract class SendTelegramChangelogTask @Inject constructor(
    private val workerExecutor: WorkerExecutor,
) : DefaultTask() {

    init {
        description = "Task to send changelog for Telegram"
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
        val currentBuildTag = fromJson(tagBuildFile.asFile.get())
        val escapedCharacters =
            "[_]|[*]|[\\[]|[\\]]|[(]|[)]|[~]|[`]|[>]|[#]|[+]|[=]|[|]|[{]|[}]|[.]|[!]"
        val changelog = changelogFile.orNull?.asFile?.readText()
        if (changelog.isNullOrEmpty()) {
            logger.error(
                "[sendChangelog] changelog file not found, is empty or error occurred"
            )
        } else {
            val telegramFormattedChangelog = changelog.formatChangelogToTelegram(escapedCharacters)
            if (telegramFormattedChangelog.isNotBlank()) {
                val slackUserMentions = userMentions.orNull.orEmpty().joinToString(", ")
                val workQueue: WorkQueue = workerExecutor.noIsolation()
                workQueue.submit(SendTelegramChangelogWork::class.java) { parameters ->
                    parameters.baseOutputFileName.set(baseOutputFileName)
                    parameters.buildName.set(currentBuildTag.name)
                    parameters.changelog.set(changelog)
                    parameters.webhookUrl.set(webhookUrl)
                    parameters.userMentions.set(slackUserMentions)
                    parameters.escapedCharacters.set(escapedCharacters)
                    parameters.botId.set(botId)
                    parameters.chatId.set(chatId)
                }
            } else {
                logger.error(
                    "[sendChangelog] changelog not sent to telegram, is empty or error occurred"
                )
            }
        }
    }

    private fun String.formatChangelogToTelegram(escapedCharacters: String): String {
        val issueUrlPrefix = issueUrlPrefix.get()
        val issueNumberPattern = issueNumberPattern.get()
        val issueRegexp = Regex(issueNumberPattern)
        return this
            .replace(escapedCharacters.toRegex()) { result -> "\\${result.value}" }
            .replace(issueRegexp) { result -> "[${result.value}](${issueUrlPrefix}${result.value})" }
            .replace(Regex("(\r\n|\n)"), "\n")
            .replace("[-]".toRegex()) { result -> "\\${result.value}" }
    }
}
