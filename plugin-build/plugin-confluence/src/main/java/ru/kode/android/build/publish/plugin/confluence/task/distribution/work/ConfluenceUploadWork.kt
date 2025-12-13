package ru.kode.android.build.publish.plugin.confluence.task.distribution.work

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.confluence.service.ConfluenceService
import ru.kode.android.build.publish.plugin.core.util.RequestError

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
}

/**
 * A Gradle work action that handles the actual file upload to Confluence.
 *
 * This class implements Gradle's WorkAction to perform the file upload asynchronously.
 * It handles the upload process and adds a comment to the Confluence page after successful upload.
 */
internal abstract class ConfluenceUploadWork : WorkAction<ConfluenceUploadParameters> {
    private val logger = Logging.getLogger(this::class.java)

    @Suppress("SwallowedException") // see logs below
    override fun execute() {
        val service = parameters.service.get()

        try {
            val distributionFile = parameters.outputFile.asFile.get()
            service.uploadFile(
                pageId = parameters.pageId.get(),
                file = distributionFile,
            )
            service.addComment(
                pageId = parameters.pageId.get(),
                fileName = distributionFile.name,
            )
        } catch (ex: RequestError.UploadTimeout) {
            logger.error(
                "Confluence upload failed with timeout exception, " +
                    "but the file was probably uploaded successfully",
                ex
            )
        }
    }
}
