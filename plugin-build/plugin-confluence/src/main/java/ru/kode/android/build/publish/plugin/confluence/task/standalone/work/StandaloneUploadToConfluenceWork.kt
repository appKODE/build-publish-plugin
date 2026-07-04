package ru.kode.android.build.publish.plugin.confluence.task.standalone.work

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import ru.kode.android.build.publish.plugin.confluence.messages.uploadingToConfluenceMessage
import ru.kode.android.build.publish.plugin.confluence.service.ConfluenceService
import ru.kode.android.build.publish.plugin.core.task.ServiceWorkParameters
import javax.inject.Inject

internal interface StandaloneUploadToConfluenceParameters : ServiceWorkParameters {
    val file: RegularFileProperty
    val pageId: Property<String>
    val service: Property<ConfluenceService>
}

internal abstract class StandaloneUploadToConfluenceWork
    @Inject
    constructor() : WorkAction<StandaloneUploadToConfluenceParameters> {
        override fun execute() {
            val file = parameters.file.asFile.get()
            val pageId = parameters.pageId.get()
            parameters.loggerService.get().info(uploadingToConfluenceMessage(file.name, pageId))
            parameters.service.get().uploadFile(
                pageId = pageId,
                file = file,
            )
        }
    }
