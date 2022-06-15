package ru.kode.android.build.publish.plugin.task.work

import groovy.json.JsonOutput
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.command.getCommandExecutor
import javax.inject.Inject

interface SendSlackChangelogParameters : WorkParameters {
    val baseOutputFileName: Property<String>
    val buildName: Property<String>
    val changelog: Property<String>
    val webhookUrl: Property<String>
    val userMentions: Property<String>
    val iconUrl: Property<String>
}

abstract class SendSlackChangelogWork @Inject constructor(
    execOperations: ExecOperations,
) : WorkAction<SendSlackChangelogParameters> {

    private val logger = Logging.getLogger(this::class.java)
    private val commandExecutor = getCommandExecutor(execOperations)

    override fun execute() {
        val baseOutputFileName = parameters.baseOutputFileName.get()
        val buildName = parameters.buildName.get()
        val body = SlackChangelogBody(
            icon_url = parameters.iconUrl.get(),
            username = "buildBot",
            blocks = listOf(
                SlackChangelogBody.Block(
                    type = "section",
                    text = SlackChangelogBody.Text(
                        type = "mrkdwn",
                        text = buildString {
                            append("*$baseOutputFileName $buildName*")
                            append(" ")
                            append(parameters.userMentions.get())
                            appendLine()
                            appendLine()
                            append(parameters.changelog.get())
                        }
                    )
                )
            )
        )
        val jsonBody = JsonOutput.toJson(body)
        val escapedJsonBody = JsonOutput.toJson(jsonBody)
        commandExecutor.sendToWebHook(parameters.webhookUrl.get(), escapedJsonBody)
        logger.debug("changelog sent to Slack")
    }
}

@Suppress("ConstructorParameterNaming") // network model
private data class SlackChangelogBody(
    val icon_url: String,
    val username: String,
    val blocks: List<Block>
) {
    data class Block(
        val type: String,
        val text: Text
    )

    data class Text(
        val type: String,
        val text: String
    )
}
