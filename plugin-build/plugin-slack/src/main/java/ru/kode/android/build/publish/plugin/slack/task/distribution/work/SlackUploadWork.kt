package ru.kode.android.build.publish.plugin.slack.task.distribution.work

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.core.util.UploadStreamTimeoutException
import ru.kode.android.build.publish.plugin.core.zip.zipFiles
import ru.kode.android.build.publish.plugin.slack.service.upload.SlackUploadService
import java.io.File

internal interface SlackUploadParameters : WorkParameters {
    val baseOutputFileName: Property<String>
    val buildName: Property<String>
    val outputFile: RegularFileProperty
    val destinationChannels: SetProperty<String>
    val networkService: Property<SlackUploadService>
}

internal abstract class SlackUploadWork : WorkAction<SlackUploadParameters> {
    private val logger = Logging.getLogger(this::class.java)

    @Suppress("SwallowedException") // see logs below
    override fun execute() {
        val uploader = parameters.networkService.get()
        val uploadFile = parameters.outputFile.asFile.get()
        val zippedUploadFile =
            listOf(uploadFile).zipFiles(
                File(
                    uploadFile.toString()
                        .replace(".${uploadFile.extension}", ".zip"),
                ),
            )
        try {
            uploader.upload(
                parameters.baseOutputFileName.get(),
                parameters.buildName.get(),
                zippedUploadFile,
                parameters.destinationChannels.get(),
            )
        } catch (ex: UploadStreamTimeoutException) {
            logger.error(
                "slack upload failed with timeout exception, " +
                    "but probably uploaded, " +
                    "see https://github.com/slackapi/python-slack-sdk/issues/1165",
            )
        }
    }
}
