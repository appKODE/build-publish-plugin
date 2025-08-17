package ru.kode.android.build.publish.plugin.slack.task.changelog.work

import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.slack.service.webhook.SlackWebhookService
import javax.inject.Inject

/**
 * Parameters required for the [SendSlackChangelogWork] task.
 *
 * This interface defines the input parameters needed to execute the Slack changelog sending.
 * It extends Gradle's [WorkParameters] to work with Gradle's Worker API.
 */
internal interface SendSlackChangelogParameters : WorkParameters {
    /**
     * The base name for the build output (e.g., app name)
     */
    val baseOutputFileName: Property<String>

    /**
     * The name/version of the build
     */
    val buildName: Property<String>

    /**
     * The actual changelog content to be sent
     */
    val changelog: Property<String>

    /**
     * Optional user mentions to be included in the message
     */
    val userMentions: Property<String>

    /**
     * URL to an icon to be displayed in the Slack message
     */
    val iconUrl: Property<String>

    /**
     *  Color code for the message attachment (e.g., "#36a64f" for green)
     */
    val attachmentColor: Property<String>

    /**
     * The network service for sending messages to Slack
     */
    val networkService: Property<SlackWebhookService>
}

/**
 * A Gradle work action that handles sending changelog messages to Slack.
 *
 * The message is formatted as a rich Slack message with:
 * - A header showing the app name and build version
 * - User mentions (if any)
 * - The changelog content in a formatted attachment
 * - Custom icon and color for better visual identification
 *
 * @see SendSlackChangelogTask The task that creates and submits this work
 * @see SlackWebhookService The service that performs the actual network communication
 */
internal abstract class SendSlackChangelogWork
    @Inject
    constructor() : WorkAction<SendSlackChangelogParameters> {
        private val logger = Logging.getLogger(this::class.java)

        override fun execute() {
            val service = parameters.networkService.get()
            service.send(
                baseOutputFileName = parameters.baseOutputFileName.get(),
                buildName = parameters.buildName.get(),
                changelog = parameters.changelog.get(),
                userMentions = parameters.userMentions.get(),
                iconUrl = parameters.iconUrl.get(),
                attachmentColor = parameters.attachmentColor.get(),
            )
            logger.info("Changelog successfully sent to Slack")
        }
    }
