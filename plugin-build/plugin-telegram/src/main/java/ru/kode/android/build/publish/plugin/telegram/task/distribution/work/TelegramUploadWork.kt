package ru.kode.android.build.publish.plugin.telegram.task.distribution.work

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.core.util.UploadStreamTimeoutException
import ru.kode.android.build.publish.plugin.telegram.service.TelegramNetworkService

internal interface TelegramUploadParameters : WorkParameters {
    val outputFile: RegularFileProperty
    val networkService: Property<TelegramNetworkService>
}

internal abstract class TelegramUploadWork : WorkAction<TelegramUploadParameters> {
    private val logger = Logging.getLogger(this::class.java)
    private val service = parameters.networkService.get()

    @Suppress("SwallowedException") // see logs below
    override fun execute() {
        try {
            service.upload(parameters.outputFile.asFile.get())
        } catch (ex: UploadStreamTimeoutException) {
            logger.error(
                "telegram upload failed with timeout exception, " +
                    "but probably uploaded",
            )
        }
    }
}
