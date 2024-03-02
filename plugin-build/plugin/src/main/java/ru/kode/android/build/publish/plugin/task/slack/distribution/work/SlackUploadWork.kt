package ru.kode.android.build.publish.plugin.task.slack.distribution.work

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.task.slack.distribution.uploader.SlackUploader
import ru.kode.android.build.publish.plugin.util.UploadStreamTimeoutException
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

interface SlackUploadParameters : WorkParameters {
    val outputFile: RegularFileProperty
    val apiToken: Property<String>
    val message: Property<String>
    val channels: SetProperty<String>
}

abstract class SlackUploadWork : WorkAction<SlackUploadParameters> {
    private val logger = Logging.getLogger(this::class.java)

    @Suppress("SwallowedException") // see logs below
    override fun execute() {
        val uploader =
            SlackUploader(
                logger,
                parameters.apiToken.get(),
            )
        val uploadFile = parameters.outputFile.asFile.get()
        val zippedUploadFile = File(uploadFile.toString().replace(".${uploadFile.extension}", ".zip"))
        zip(listOf(uploadFile), zippedUploadFile)
        try {
            uploader.upload(
                zippedUploadFile,
                parameters.channels.get(),
                parameters.message.orNull,
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

@Suppress("NestedBlockDepth", "MagicNumber") // simple zip logic
private fun zip(files: List<File>, zipFile: File): File {
    ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { out ->
        val data = ByteArray(1024)
        for (file in files) {
            FileInputStream(file).use { fi ->
                BufferedInputStream(fi).use { origin ->
                    val entry = ZipEntry(file.name)
                    out.putNextEntry(entry)
                    while (true) {
                        val readBytes = origin.read(data)
                        if (readBytes == -1) {
                            break
                        }
                        out.write(data, 0, readBytes)
                    }
                }
            }
        }
    }
    return zipFile
}
