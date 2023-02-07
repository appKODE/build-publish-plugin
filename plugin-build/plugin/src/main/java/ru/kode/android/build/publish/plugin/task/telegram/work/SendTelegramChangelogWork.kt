package ru.kode.android.build.publish.plugin.task.telegram.work

import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.task.telegram.sender.TelegramWebhookSender
import java.net.URLEncoder
import javax.inject.Inject

interface SendTelegramChangelogParameters : WorkParameters {
    val baseOutputFileName: Property<String>
    val buildName: Property<String>
    val changelog: Property<String>
    val webhookUrl: Property<String>
    val userMentions: Property<String>
    val escapedCharacters: Property<String>
    val botId: Property<String>
    val chatId: Property<String>
}

abstract class SendTelegramChangelogWork @Inject constructor() : WorkAction<SendTelegramChangelogParameters> {

    private val logger = Logging.getLogger(this::class.java)
    private val webhookSender = TelegramWebhookSender(logger)

    override fun execute() {
        val baseOutputFileName = parameters.baseOutputFileName.get()
        val buildName = parameters.buildName.get()
        val tgUserMentions = parameters.userMentions.get()
        val escapedHeader = "$baseOutputFileName $buildName"
            .replace(parameters.escapedCharacters.get().toRegex()) { result -> "\\${result.value}" }
        val boldHeader = "*$escapedHeader*"
        val message = buildString {
            append(boldHeader)
            appendLine()
            append(tgUserMentions)
            appendLine()
            appendLine()
            append(parameters.changelog.get())
        }.formatChangelog()
        val url = parameters.webhookUrl.get().format(
            parameters.botId.get(),
            parameters.chatId.get(),
            URLEncoder.encode(message, "utf-8")
        )
        webhookSender.send(url)
        logger.debug("changelog sent to Telegram")
    }
}

private fun String.formatChangelog(): String {
    return this
        .replace(Regex("(\r\n|\n)"), "\n")
}
