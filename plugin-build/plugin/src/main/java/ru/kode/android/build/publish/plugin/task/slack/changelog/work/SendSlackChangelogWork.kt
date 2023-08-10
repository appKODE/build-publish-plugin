package ru.kode.android.build.publish.plugin.task.slack.changelog.work

import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.task.slack.changelog.entity.SlackChangelogBody
import ru.kode.android.build.publish.plugin.task.slack.changelog.sender.SlackWebhookSender
import javax.inject.Inject

interface SendSlackChangelogParameters : WorkParameters {
    val baseOutputFileName: Property<String>
    val buildName: Property<String>
    val changelog: Property<String>
    val webhookUrl: Property<String>
    val userMentions: Property<String>
    val iconUrl: Property<String>
}

abstract class SendSlackChangelogWork @Inject constructor() : WorkAction<SendSlackChangelogParameters> {

    private val logger = Logging.getLogger(this::class.java)
    private val webhookSender = SlackWebhookSender(logger)

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
        webhookSender.send(parameters.webhookUrl.get(), body)
        logger.info("changelog sent to Slack")
    }
}
