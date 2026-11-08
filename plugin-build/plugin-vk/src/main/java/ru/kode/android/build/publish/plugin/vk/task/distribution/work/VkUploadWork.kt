package ru.kode.android.build.publish.plugin.vk.task.distribution.work

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.core.zip.zipped
import ru.kode.android.build.publish.plugin.vk.messages.documentSentMessage
import ru.kode.android.build.publish.plugin.vk.messages.uploadFailedMessage
import ru.kode.android.build.publish.plugin.vk.service.VkService
import ru.kode.android.gradle.commons.logger.LoggerService

interface VkUploadParameters : WorkParameters {
    val outputFile: RegularFileProperty
    val peerId: Property<Long>
    val title: Property<String>
    val message: Property<String>
    val compressed: Property<Boolean>
    val service: Property<VkService>
    val loggerService: Property<LoggerService>
}

internal abstract class VkUploadWork : WorkAction<VkUploadParameters> {
    override fun execute() {
        val logger = parameters.loggerService.get()
        val outputFile = parameters.outputFile.asFile.get()
        val distributionFile = if (parameters.compressed.orElse(false).get()) outputFile.zipped() else outputFile
        val peerId = parameters.peerId.get()

        runCatching {
            val result =
                parameters.service.get().sendDocument(
                    peerId = peerId,
                    file = distributionFile,
                    title = parameters.title.get(),
                    message = parameters.message.orNull,
                )
            logger.quiet(documentSentMessage(peerId, result.messageId, result.documentUrl))
        }.onFailure { ex ->
            logger.error(uploadFailedMessage(), ex)
        }.getOrThrow()
    }
}
