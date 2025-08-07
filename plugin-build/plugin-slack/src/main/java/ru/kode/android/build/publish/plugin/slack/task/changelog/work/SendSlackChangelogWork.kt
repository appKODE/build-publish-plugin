package ru.kode.android.build.publish.plugin.slack.task.changelog.work

import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.slack.service.webhook.SlackWebhookService
import javax.inject.Inject

internal interface SendSlackChangelogParameters : WorkParameters {
    val baseOutputFileName: Property<String>
    val buildName: Property<String>
    val changelog: Property<String>
    val userMentions: Property<String>
    val iconUrl: Property<String>
    val attachmentColor: Property<String>
    val networkService: Property<SlackWebhookService>
}

internal abstract class SendSlackChangelogWork
    @Inject
    constructor() : WorkAction<SendSlackChangelogParameters> {
        private val logger = Logging.getLogger(this::class.java)
        private val service = parameters.networkService.get()

        override fun execute() {
            service.send(
                baseOutputFileName = parameters.baseOutputFileName.get(),
                buildName = parameters.buildName.get(),
                changelog = parameters.changelog.get(),
                userMentions = parameters.userMentions.get(),
                iconUrl = parameters.iconUrl.get(),
                attachmentColor = parameters.attachmentColor.get(),
            )
            logger.info("changelog sent to Slack")
        }
    }
