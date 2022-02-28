package ru.kode.android.firebase.publish.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import ru.kode.android.firebase.publish.plugin.command.LinuxShellCommandExecutor
import ru.kode.android.firebase.publish.plugin.error.ValueNotFoundException
import ru.kode.android.firebase.publish.plugin.git.GitRepository
import ru.kode.android.firebase.publish.plugin.git.entity.Tag
import ru.kode.android.firebase.publish.plugin.util.Changelog
import java.net.URLEncoder

data class TelegramSendConfig(
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

data class SlackSendConfig(
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

    private val commandExecutor = LinuxShellCommandExecutor(project)

    @get:Input
    @get:Option(option = "buildVariants", description = "List of all available build variants")
    abstract val buildVariants: SetProperty<String>

    @get:Input
    @get:Option(
        option = "commitMessageKey",
        description = "Message key to collect interested commits"
    )
    abstract val commitMessageKey: Property<String>

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
    fun prepareChangelog() {
        val buildVariants = buildVariants.get()
        val escapedCharacters =
            "[_]|[*]|[\\[]|[\\]]|[(]|[)]|[~]|[`]|[>]|[#]|[+]|[=]|[|]|[{]|[}]|[.]|[!]".toRegex()
        val escapedChangelogMessage = prepareChangelog(buildVariants, escapedCharacters)
        if (escapedChangelogMessage.isNullOrBlank()) {
            project.logger.error("[sendChangelog] changelog not sent, is empty or error occurred")
        }
        val buildTag = getBuildTag(buildVariants)
        if (!tgConfig.orNull.isNullOrEmpty()) {
            val tgConfig = TelegramSendConfig(tgConfig.get())
            val botId = tgConfig.botId
                .takeIf { it.isNotBlank() }
                .also { if (it == null) logger.warn("Bot ID is not set. Skipping changelog task") }
                ?: return

            val baseOutputFileName = baseOutputFileName.get()
            val tgUserMentions = tgUserMentions.orNull.orEmpty().joinToString(", ")
            val text = (
                    "$baseOutputFileName ${buildTag.name}\n" +
                            "${tgUserMentions}\n\n"
                    )
                .replace(escapedCharacters) { result -> "\\${result.value}" }
                .replace("[-]".toRegex()) { result -> "\\${result.value}" }

            val url = tgConfig.webhookUrl.format(
                botId,
                tgConfig.chatId,
                URLEncoder.encode("$text$escapedChangelogMessage", "utf-8")
            )
            commandExecutor.sendToWebHook(url)
            project.logger.debug("changelog sent to Telegram")
        }
        if (!slackConfig.orNull.isNullOrEmpty()) {
            val slackConfig = SlackSendConfig(slackConfig.get())
            val slackUserMentions = slackUserMentions.orNull.orEmpty().joinToString(", ")
            val json = "\"{" +
                    "\\\"icon_url\\\": \\\"${slackConfig.iconUrl}\\\"," +
                    "\\\"username\\\": \\\"buildBot\\\", " +
                    "\\\"blocks\\\": [ " +
                    "{ \\\"type\\\": \\\"section\\\", \\\"text\\\": { " +
                    "\\\"type\\\": \\\"mrkdwn\\\", " +
                    "\\\"text\\\": \\\"*${baseOutputFileName} ${buildTag.name}*" +
                    " $slackUserMentions\\n\\n$escapedChangelogMessage\\\" " +
                    "}" +
                    "} ]" +
                    "}\""
            commandExecutor.sendToWebHook(slackConfig.webhookUrl, json)
            project.logger.debug("changelog sent to Slack")
        }
        if (tgConfig.orNull.isNullOrEmpty() && slackConfig.orNull.isNullOrEmpty()) {
            project.logger.debug("changelog:")
            project.logger.debug(escapedChangelogMessage)
        }
    }

    private fun prepareChangelog(
        buildVariants: Set<String>,
        escapedCharacters: Regex,
    ): String? {
        val messageKey = commitMessageKey.get()
        val issueUrlPrefix = issueUrlPrefix.get()
        val issueNumberPattern = issueNumberPattern.get()

        val changelog = Changelog(commandExecutor, project.logger, messageKey, buildVariants)
            .buildForRecentBuildTag(
                defaultValueSupplier = { tagRange ->
                    val previousBuildName = tagRange.previousBuildTag?.name?.let { "(**$it**)" }
                    "No changes in comparison with a previous build $previousBuildName"
                }
            )
        val issueRegexp = Regex(issueNumberPattern)
        return changelog
            ?.replace(escapedCharacters) { result -> "\\${result.value}" }
            ?.replace(issueRegexp) { result -> "[${result.value}](${issueUrlPrefix}${result.value})" }
            ?.replace(Regex("(\r\n|\n)"), "\n")
            ?.replace("[-]".toRegex()) { result -> "\\${result.value}" }
    }

    private fun getBuildTag(buildVariants: Set<String>): Tag.Build {
        return GitRepository(commandExecutor, buildVariants)
            .findMostRecentBuildTag()
            ?: throw GradleException("unable to send changelog: failed to find most recent build tag")
    }
}
