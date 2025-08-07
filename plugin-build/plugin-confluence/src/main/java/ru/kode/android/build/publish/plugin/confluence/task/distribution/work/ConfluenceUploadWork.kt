package ru.kode.android.build.publish.plugin.confluence.task.distribution.work

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.confluence.service.network.ConfluenceNetworkService
import ru.kode.android.build.publish.plugin.core.util.UploadStreamTimeoutException

interface ConfluenceUploadParameters : WorkParameters {
    val outputFile: RegularFileProperty
    val pageId: Property<String>
    val fileName: Property<String>
    val networkService: Property<ConfluenceNetworkService>
}

internal abstract class ConfluenceUploadWork : WorkAction<ConfluenceUploadParameters> {
    private val logger = Logging.getLogger(this::class.java)
    private val uploader = parameters.networkService.get()

    @Suppress("SwallowedException") // see logs below
    override fun execute() {
        try {
            uploader.uploadFile(
                pageId = parameters.pageId.get(),
                file = parameters.outputFile.asFile.get(),
            )
            uploader.addComment(
                pageId = parameters.pageId.get(),
                fileName = parameters.fileName.get(),
            )
        } catch (ex: UploadStreamTimeoutException) {
            logger.error(
                "Confluence upload failed with timeout exception, " +
                    "but probably uploaded",
            )
        }
    }
}
