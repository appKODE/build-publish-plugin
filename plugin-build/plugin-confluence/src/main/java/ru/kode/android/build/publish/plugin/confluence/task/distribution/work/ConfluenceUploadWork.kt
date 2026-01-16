package ru.kode.android.build.publish.plugin.confluence.task.distribution.work

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.confluence.messages.uploadFailedMessage
import ru.kode.android.build.publish.plugin.confluence.service.ConfluenceService
import ru.kode.android.build.publish.plugin.core.logger.LoggerService
import ru.kode.android.build.publish.plugin.core.util.RequestError
import ru.kode.android.build.publish.plugin.core.zip.zipped

/**
 * Parameters required for the Confluence upload work action.
 */
interface ConfluenceUploadParameters : WorkParameters {
    /**
     * The file to be uploaded to Confluence
     */
    val outputFile: RegularFileProperty

    /**
     * The ID of the Confluence page where the file should be uploaded
     */
    val pageId: Property<String>

    /**
     * The network service used to communicate with Confluence
     */
    val service: Property<ConfluenceService>

    /**
     * The logger service used to log messages during the upload process.
     */
    val loggerService: Property<LoggerService>

    /**
     * Whether to compress the file before uploading to Confluence.
     */
    val compressed: Property<Boolean>
}

/**
 * A Gradle work action that handles the actual file upload to Confluence.
 *
 * This class implements Gradle's WorkAction to perform the file upload asynchronously.
 * It handles the upload process and adds a comment to the Confluence page after successful upload.
 */
internal abstract class ConfluenceUploadWork : WorkAction<ConfluenceUploadParameters> {
    @Suppress("SwallowedException") // see logs below
    override fun execute() {
        val service = parameters.service.get()
        val logger = parameters.loggerService.get()
        val compressed = parameters.compressed.orElse(false).get()

        try {
            val outputFile = parameters.outputFile.asFile.get()
            val distributionFile = if (compressed) outputFile.zipped() else outputFile
            service.uploadFile(
                pageId = parameters.pageId.get(),
                file = distributionFile,
            )
            service.addComment(
                pageId = parameters.pageId.get(),
                fileName = distributionFile.name,
            )
        } catch (ex: RequestError.UploadTimeout) {
            logger.error(uploadFailedMessage(), ex)
        }
    }
}
