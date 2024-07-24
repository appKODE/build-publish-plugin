package ru.kode.android.build.publish.plugin.task.telegram.distribution.work

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.task.telegram.distribution.uploader.TelegramUploader
import ru.kode.android.build.publish.plugin.util.UploadStreamTimeoutException

interface TelegramUploadParameters : WorkParameters {
    val outputFile: RegularFileProperty
    val botId: Property<String>
    val chatId: Property<String>
    val topicId: Property<String>
}

abstract class TelegramUploadWork : WorkAction<TelegramUploadParameters> {
    private val logger = Logging.getLogger(this::class.java)
    private val uploader = TelegramUploader(logger)

    @Suppress("SwallowedException") // see logs below
    override fun execute() {
        try {
            val url = SEND_DOCUMENT_WEB_HOOK.format(parameters.botId.get())
            uploader.upload(
                url,
                parameters.chatId.get(),
                parameters.topicId.orNull,
                parameters.outputFile.asFile.get(),
            )
        } catch (ex: UploadStreamTimeoutException) {
            logger.error(
                "telegram upload failed with timeout exception, " +
                    "but probably uploaded",
            )
        }
    }
}

private const val SEND_DOCUMENT_WEB_HOOK = "https://api.telegram.org/bot%s/sendDocument"
