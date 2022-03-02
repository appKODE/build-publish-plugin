package ru.kode.android.build.publish.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import ru.kode.android.build.publish.plugin.command.LinuxShellCommandExecutor
import ru.kode.android.build.publish.plugin.error.ValueNotFoundException
import ru.kode.android.build.publish.plugin.git.GitRepository
import ru.kode.android.build.publish.plugin.git.entity.Tag
import ru.kode.android.build.publish.plugin.util.Changelog
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
    fun prepareTgChangelog() {
        val buildVariants = buildVariants.get()
        val buildTag = getBuildTag(buildVariants)

        sendTelegramChangelog(buildVariants, buildTag)
        sendSlackMessage(buildVariants, buildTag)
        sendLocalChangelog(buildVariants)
    }

    private fun sendLocalChangelog(buildVariants: Set<String>) {
        if (tgConfig.orNull.isNullOrEmpty() && slackConfig.orNull.isNullOrEmpty()) {
            val messageKey = commitMessageKey.get()
            val changelog = Changelog(commandExecutor, project.logger, messageKey, buildVariants)
                .buildForRecentBuildTag(
                    defaultValueSupplier = { tagRange ->
                        val previousBuildName = tagRange.previousBuildTag?.name?.let { "(**$it**)" }
                        "No changes in comparison with a previous build $previousBuildName"
                    }
                )
            project.logger.debug("changelog:")
            project.logger.debug(changelog)
        }
    }

    private fun sendTelegramChangelog(buildVariants: Set<String>, buildTag: Tag.Build) {
        val escapedCharacters =
            "[_]|[*]|[\\[]|[\\]]|[(]|[)]|[~]|[`]|[>]|[#]|[+]|[=]|[|]|[{]|[}]|[.]|[!]".toRegex()
        val tgChangelogMessage = prepareTgChangelog(buildVariants, escapedCharacters)
        if (tgChangelogMessage.isNullOrBlank()) {
            project.logger.error(
                "[sendChangelog] changelog not sent to telegram, is empty or error occurred"
            )
        }
        if (!tgConfig.orNull.isNullOrEmpty()) {
            project.logger.debug("telegram config: ${tgConfig.get()}, mentions $tgUserMentions")
            val tgConfig = TelegramSendConfig(tgConfig.get())
            val baseOutputFileName = baseOutputFileName.get()
            val tgUserMentions = tgUserMentions.orNull.orEmpty().joinToString(", ")
            val text = (
                "$baseOutputFileName ${buildTag.name}\n" +
                    "${tgUserMentions}\n\n"
                )
                .replace(escapedCharacters) { result -> "\\${result.value}" }
                .replace("[-]".toRegex()) { result -> "\\${result.value}" }

            val url = tgConfig.webhookUrl.format(
                tgConfig.botId,
                tgConfig.chatId,
                URLEncoder.encode("$text$tgChangelogMessage", "utf-8")
            )
            commandExecutor.sendToWebHook(url)
            project.logger.debug("changelog sent to Telegram")
        }
    }

    private fun sendSlackMessage(buildVariants: Set<String>, buildTag: Tag.Build) {
        val slackChangelogMessage = prepareSlackChangelog(buildVariants)
        if (slackChangelogMessage.isNullOrBlank()) {
            project.logger.error(
                "[sendChangelog] changelog not sent to Slack, is empty or error occurred"
            )
        }
        if (!slackConfig.orNull.isNullOrEmpty()) {
            project.logger.debug("slack config: ${slackConfig.get()}, mentions $slackUserMentions")

            val slackConfig = SlackSendConfig(slackConfig.get())
            val slackUserMentions = slackUserMentions.orNull.orEmpty().joinToString(", ")
            val baseOutputFileName = baseOutputFileName.get()
            val json = "\"{" +
                "\\\"icon_url\\\": \\\"${slackConfig.iconUrl}\\\"," +
                "\\\"username\\\": \\\"buildBot\\\", " +
                "\\\"blocks\\\": [ " +
                "{ \\\"type\\\": \\\"section\\\", \\\"text\\\": { " +
                "\\\"type\\\": \\\"mrkdwn\\\", " +
                "\\\"text\\\": \\\"*$baseOutputFileName ${buildTag.name}*" +
                " $slackUserMentions\\n\\n$slackChangelogMessage\\\" " +
                "}" +
                "} ]" +
                "}\""

            commandExecutor.sendToWebHook(slackConfig.webhookUrl, json)
            project.logger.debug("changelog sent to Slack")
        }
    }

    private fun prepareTgChangelog(
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

    private fun prepareSlackChangelog(
        buildVariants: Set<String>,
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
        return changelog
            ?.replace(Regex(issueNumberPattern), "<$issueUrlPrefix\$0|\$0>")
            ?.replace(Regex("(\r\n|\n)"), "\\\\n")
            // only this insane amount of quotes works! they are needed to produce \\\" in json
            ?.replace(Regex("\""), "\\\\\\\\\\\\\"")
    }

    private fun getBuildTag(buildVariants: Set<String>): Tag.Build {
        return GitRepository(commandExecutor, buildVariants)
            .findMostRecentBuildTag()
            ?: throw GradleException("unable to send changelog: failed to find most recent build tag")
    }
}
