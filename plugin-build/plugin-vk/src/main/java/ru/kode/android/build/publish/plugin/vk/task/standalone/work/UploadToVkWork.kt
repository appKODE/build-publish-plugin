package ru.kode.android.build.publish.plugin.vk.task.standalone.work

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import ru.kode.android.build.publish.plugin.core.task.ServiceWorkParameters
import ru.kode.android.build.publish.plugin.vk.messages.documentSentMessage
import ru.kode.android.build.publish.plugin.vk.service.VkService
import javax.inject.Inject

internal interface UploadToVkParameters : ServiceWorkParameters {
    val file: RegularFileProperty
    val peerId: Property<Long>
    val title: Property<String>
    val message: Property<String>
    val service: Property<VkService>
}

internal abstract class UploadToVkWork
    @Inject
    constructor() : WorkAction<UploadToVkParameters> {
        override fun execute() {
            val peerId = parameters.peerId.get()
            val result =
                parameters.service.get().sendDocument(
                    peerId = peerId,
                    file = parameters.file.asFile.get(),
                    title = parameters.title.get(),
                    message = parameters.message.orNull,
                )
            parameters.loggerService.get().quiet(documentSentMessage(peerId, result.messageId, result.documentUrl))
        }
    }
