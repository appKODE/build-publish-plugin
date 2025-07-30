package ru.kode.android.build.publish.plugin.confluence.task.distribution.work

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.confluence.task.distribution.uploader.ConfluenceUploader
import ru.kode.android.build.publish.plugin.core.util.UploadStreamTimeoutException

interface ConfluenceUploadParameters : WorkParameters {
    val outputFile: RegularFileProperty
    val username: Property<String>
    val password: Property<String>
    val pageId: Property<String>
    val fileName: Property<String>
}

abstract class ConfluenceUploadWork : WorkAction<ConfluenceUploadParameters> {
    private val logger = Logging.getLogger(this::class.java)
    private val uploader = ConfluenceUploader(logger)

    @Suppress("SwallowedException") // see logs below
    override fun execute() {
        try {
            uploader.uploadFile(
                username = parameters.username.get(),
                password = parameters.password.get(),
                pageId = parameters.pageId.get(),
                file = parameters.outputFile.asFile.get(),
            )
            uploader.addComment(
                username = parameters.username.get(),
                password = parameters.password.get(),
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
