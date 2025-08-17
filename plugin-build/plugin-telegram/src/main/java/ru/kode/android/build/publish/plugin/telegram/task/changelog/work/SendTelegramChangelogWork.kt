package ru.kode.android.build.publish.plugin.telegram.task.changelog.work

import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.telegram.config.DestinationBot
import ru.kode.android.build.publish.plugin.telegram.service.network.TelegramNetworkService
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
    val userMentions: Property<String>

    /**
     * String containing characters that need to be escaped in Telegram messages
     */
    val escapedCharacters: Property<String>

    /**
     * The network service for sending messages to Telegram
     */
    val networkService: Property<TelegramNetworkService>

    /**
     * Set of Telegram bot configurations and their destination chats
     */
    val destinationBots: SetProperty<DestinationBot>
}

/**
 * A Gradle work action that handles sending changelog messages to Telegram.
 *
 * This class implements [WorkAction] to perform the changelog sending asynchronously
 * using Gradle's Worker API. It's designed to be used by [SendTelegramChangelogTask]
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
 * @see SendTelegramChangelogTask The task that creates and submits this work
 * @see TelegramNetworkService The service that performs the actual network communication
 */
internal abstract class SendTelegramChangelogWork
    @Inject
    constructor() : WorkAction<SendTelegramChangelogParameters> {
        private val logger = Logging.getLogger(this::class.java)

        override fun execute() {
            val service = parameters.networkService.get()

            val baseOutputFileName = parameters.baseOutputFileName.get()
            val buildName = parameters.buildName.get()
            val tgUserMentions = parameters.userMentions.get()

            val escapedHeader =
                "$baseOutputFileName $buildName"
                    .replace(parameters.escapedCharacters.get().toRegex()) { result -> "\\${result.value}" }

            val boldHeader = "*$escapedHeader*"

            val message =
                buildString {
                    append(boldHeader)
                    appendLine()
                    append(tgUserMentions)
                    appendLine()
                    appendLine()
                    append(parameters.changelog.get())
                }.formatChangelog()

            service.send(message, parameters.destinationBots.get())
            logger.info("Changelog successfully sent to Telegram")
        }
    }

private fun String.formatChangelog(): String {
    return this.replace(Regex("(\r\n|\r|\n)"), "\n")
}
