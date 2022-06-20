package ru.kode.android.build.publish.plugin.task.work

import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.command.getCommandExecutor
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

abstract class SendTelegramChangelogWork @Inject constructor(
    execOperations: ExecOperations,
) : WorkAction<SendTelegramChangelogParameters> {

    private val logger = Logging.getLogger(this::class.java)
    private val commandExecutor = getCommandExecutor(execOperations)

    override fun execute() {
        val baseOutputFileName = parameters.baseOutputFileName.get()
        val buildName = parameters.buildName.get()
        val tgUserMentions = parameters.userMentions.get()
        val message = buildString {
            append(baseOutputFileName)
            append(" ")
            append(buildName)
            appendLine()
            append(tgUserMentions)
            appendLine()
            appendLine()
            append(parameters.changelog.get())
        }
            .replace(parameters.escapedCharacters.get().toRegex()) { result -> "\\${result.value}" }
            .replace("[-]".toRegex()) { result -> "\\${result.value}" }
        val url = parameters.webhookUrl.get().format(
            parameters.botId.get(),
            parameters.chatId.get(),
            URLEncoder.encode(message, "utf-8")
        )
        commandExecutor.sendToWebHook(url)
        logger.debug("changelog sent to Telegram")
    }
}
