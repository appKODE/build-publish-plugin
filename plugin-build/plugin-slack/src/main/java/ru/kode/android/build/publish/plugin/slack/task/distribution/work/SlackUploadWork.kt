package ru.kode.android.build.publish.plugin.slack.task.distribution.work

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.core.util.RequestError
import ru.kode.android.build.publish.plugin.core.zip.zipAllInto
import ru.kode.android.build.publish.plugin.slack.service.upload.SlackUploadService
import java.io.File

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
    val networkService: Property<SlackUploadService>
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
    private val logger = Logging.getLogger(this::class.java)

    @Suppress("SwallowedException") // see logs below
    override fun execute() {
        val uploader = parameters.networkService.get()
        val distributionFile = parameters.distributionFile.asFile.get()
        val zippedDistributionFile =
            listOf(distributionFile).zipAllInto(
                File(
                    distributionFile.toString()
                        .replace(".${distributionFile.extension}", ".zip"),
                ),
            )
        try {
            uploader.upload(
                parameters.baseOutputFileName.get(),
                parameters.buildName.get(),
                zippedDistributionFile,
                parameters.destinationChannels.get(),
            )
        } catch (ex: RequestError.UploadTimeout) {
            logger.error(
                "slack upload failed with timeout exception, " +
                    "but probably uploaded, " +
                    "see https://github.com/slackapi/python-slack-sdk/issues/1165",
            )
        }
    }
}
