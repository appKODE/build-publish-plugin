package ru.kode.android.build.publish.plugin.telegram.task.distribution.work

import okhttp3.Credentials
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.telegram.task.distribution.uploader.TelegramUploader
import ru.kode.android.build.publish.plugin.core.util.UploadStreamTimeoutException

interface TelegramUploadParameters : WorkParameters {
    val outputFile: RegularFileProperty
    val botId: Property<String>
    val chatId: Property<String>
    val topicId: Property<String>
    val botBaseUrl: Property<String>
    val botAuthUsername: Property<String>
    val botAuthPassword: Property<String>
}

abstract class TelegramUploadWork : WorkAction<TelegramUploadParameters> {
    private val logger = Logging.getLogger(this::class.java)
    private val uploader = TelegramUploader(logger)

    @Suppress("SwallowedException") // see logs below
    override fun execute() {
        try {
            val url =
                SEND_DOCUMENT_WEB_HOOK.format(
                    parameters.botBaseUrl.getOrElse(DEFAULT_BASE_URL),
                    parameters.botId.get(),
                )
            val authorization =
                parameters.botAuthUsername
                    .zip(parameters.botAuthPassword) { userName, password ->
                        Credentials.basic(userName, password)
                    }
                    .orNull
            uploader.upload(
                url,
                parameters.chatId.get(),
                parameters.topicId.orNull,
                parameters.outputFile.asFile.get(),
                authorization,
            )
        } catch (ex: UploadStreamTimeoutException) {
            logger.error(
                "telegram upload failed with timeout exception, " +
                    "but probably uploaded",
            )
        }
    }
}

private const val DEFAULT_BASE_URL = "https://api.telegram.org"
private const val SEND_DOCUMENT_WEB_HOOK = "%s/bot%s/sendDocument"
