package ru.kode.android.build.publish.plugin.telegram.task.changelog.work

import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.telegram.controller.mappers.destinationTelegramBotsFromJson
import ru.kode.android.build.publish.plugin.telegram.messages.changelogSentMessage
import ru.kode.android.build.publish.plugin.telegram.service.TelegramService
import javax.inject.Inject

/**
 * Parameters required for the [SendTelegramChangelogWork] task.
 *
 * This interface defines the input parameters needed to execute the Telegram changelog sending.
 * It extends Gradle's [WorkParameters] to work with Gradle's Worker API.
 */
internal interface SendTelegramChangelogParameters : WorkParameters {
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
    val userMentions: SetProperty<String>

    /**
     * The URL prefix for issue tracker links.
     */
    val issueUrlPrefix: Property<String>

    /**
     * The regular expression pattern used to identify issue references in the changelog text.
     */
    val issueNumberPattern: Property<String>

    /**
     * Set of Telegram bot configurations and their destination chats
     */
    val destinationBots: Property<String>

    /**
     * The network service for sending messages to Telegram
     */
    val service: Property<TelegramService>
}

/**
 * A Gradle work action that handles sending changelog messages to Telegram.
 *
 * This class implements [WorkAction] to perform the changelog sending asynchronously
 * using Gradle's Worker API. It's designed to be used by [ru.kode.android.build.publish.plugin.telegram.task.changelog.SendTelegramChangelogTask]
 * to offload potentially long-running network operations to a separate thread.
 *
 * The message format is as follows:
 * ```
 * *AppName 1.0.0*
 * @user1 @user2
 *
 * - Fixed issue #123
 * - Added new feature
 * ```
 *
 * @see ru.kode.android.build.publish.plugin.telegram.task.changelog.SendTelegramChangelogTask The task that creates and submits this work
 * @see TelegramService The service that performs the actual network communication
 */
internal abstract class SendTelegramChangelogWork
    @Inject
    constructor() : WorkAction<SendTelegramChangelogParameters> {
        private val logger = Logging.getLogger(this::class.java)

        override fun execute() {
            val service = parameters.service.get()

            val baseOutputFileName = parameters.baseOutputFileName.get()
            val buildName = parameters.buildName.get()
            val header = "$baseOutputFileName $buildName"

            service.send(
                changelog = parameters.changelog.get(),
                header = header,
                userMentions = parameters.userMentions.orNull?.toList().orEmpty(),
                issueUrlPrefix = parameters.issueUrlPrefix.get(),
                issueNumberPattern = parameters.issueNumberPattern.get(),
                destinationBots = parameters.destinationBots
                    .map { destinationTelegramBotsFromJson(it) }
                    .get()
            )
            logger.info(changelogSentMessage())
        }
    }
