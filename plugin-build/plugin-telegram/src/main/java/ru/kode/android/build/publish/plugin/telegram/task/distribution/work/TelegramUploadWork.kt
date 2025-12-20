package ru.kode.android.build.publish.plugin.telegram.task.distribution.work

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.core.util.RequestError
import ru.kode.android.build.publish.plugin.telegram.controller.mappers.destinationTelegramBotsFromJson
import ru.kode.android.build.publish.plugin.telegram.messages.telegramUploadFailedMessage
import ru.kode.android.build.publish.plugin.telegram.service.TelegramService

/**
 * Parameters required for the [TelegramUploadWork] task.
 *
 * This interface defines the input parameters needed to execute the Telegram file upload.
 * It extends Gradle's [WorkParameters] to work with Gradle's Worker API.
 */
internal interface TelegramUploadParameters : WorkParameters {
    /**
     * The file to be uploaded to Telegram
     */
    val distributionFile: RegularFileProperty

    /**
     * Set of Telegram bot configurations and their destination chats
     */
    val destinationBots: Property<String>

    /**
     * The network service instance for handling Telegram API communication
     */
    val service: Property<TelegramService>
}

/**
 * A Gradle work action that handles the actual file upload to Telegram.
 *
 * This class implements [WorkAction] to perform the file upload asynchronously
 * using Gradle's Worker API. It's designed to be used by [TelegramDistributionTask]
 * to offload potentially long-running upload operations to a separate thread.
 *
 * ## Error Handling
 * - Catches and logs [RequestError.UploadTimeout] specifically, as these might occur
 *   even when the upload was successful on Telegram's side.
 * - Other exceptions will bubble up and be handled by Gradle's task execution framework.
 *
 * @see TelegramDistributionTask The task that creates and submits this work
 * @see TelegramService The service that performs the actual network communication
 */
internal abstract class TelegramUploadWork : WorkAction<TelegramUploadParameters> {
    private val logger = Logging.getLogger(this::class.java)

    @Suppress("SwallowedException")
    override fun execute() {
        val service = parameters.service.get()
        try {
            service.upload(
                parameters.distributionFile.asFile.get(),
                destinationBots = parameters.destinationBots.map { destinationTelegramBotsFromJson(it) }.get(),
            )
        } catch (ex: RequestError.UploadTimeout) {
            logger.error(telegramUploadFailedMessage(), ex)
        }
    }
}
