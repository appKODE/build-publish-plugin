package ru.kode.android.build.publish.plugin.slack.task.distribution.work

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.core.logger.LoggerService
import ru.kode.android.build.publish.plugin.core.util.RequestError
import ru.kode.android.build.publish.plugin.slack.messages.uploadFailedMessage
import ru.kode.android.build.publish.plugin.slack.service.SlackService

/**
 * Parameters for the Slack upload work action.
 *
 * This interface defines the input parameters required for the [SlackUploadWork] action.
 */
internal interface SlackUploadParameters : WorkParameters {
    /**
     * Base name for the uploaded file
     */
    val baseOutputFileName: Property<String>

    /**
     * Name or identifier of the build
     */
    val buildName: Property<String>

    /**
     * The file to be uploaded to Slack
     */
    val distributionFile: RegularFileProperty

    /**
     * Set of channel names or IDs where the file should be shared
     */
    val destinationChannels: SetProperty<String>

    /**
     * The Slack upload service to use for the file upload
     */
    val service: Property<SlackService>

    /**
     * The logger service to use for logging debug and error messages.
     */
    val loggerService: Property<LoggerService>
}

/**
 * A Gradle work action that handles file uploads to Slack in a background thread.
 *
 * This work action is responsible for:
 * - Zipping the distribution file if needed
 * - Uploading the file to Slack using the provided service
 * - Handling upload timeouts and errors
 * - Logging the upload status
 *
 * The actual upload is performed asynchronously by Gradle's worker API.
 */
internal abstract class SlackUploadWork : WorkAction<SlackUploadParameters> {
    @Suppress("SwallowedException") // see logs below
    override fun execute() {
        val service = parameters.service.get()
        val logger = parameters.loggerService.get()
        val distributionFile = parameters.distributionFile.asFile.get()
        try {
            val baseOutputFileName = parameters.baseOutputFileName.get()
            val buildName = parameters.buildName.get()
            val initialComment = "$baseOutputFileName $buildName"

            service.upload(
                initialComment,
                distributionFile,
                parameters.destinationChannels.get().toList(),
            )
        } catch (ex: RequestError.UploadTimeout) {
            logger.error(uploadFailedMessage(), ex)
        }
    }
}
