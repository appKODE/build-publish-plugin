package ru.kode.android.build.publish.plugin.confluence.task.standalone.work

import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import ru.kode.android.build.publish.plugin.confluence.messages.addingCommentToConfluenceMessage
import ru.kode.android.build.publish.plugin.confluence.service.ConfluenceService
import ru.kode.android.build.publish.plugin.core.task.ServiceWorkParameters
import javax.inject.Inject

internal interface StandaloneAddConfluenceCommentParameters : ServiceWorkParameters {
    val pageId: Property<String>
    val fileName: Property<String>
    val service: Property<ConfluenceService>
}

internal abstract class StandaloneAddConfluenceCommentWork
    @Inject
    constructor() : WorkAction<StandaloneAddConfluenceCommentParameters> {
        override fun execute() {
            val pageId = parameters.pageId.get()
            val fileName = parameters.fileName.get()
            parameters.loggerService.get().info(addingCommentToConfluenceMessage(fileName, pageId))
            parameters.service.get().addComment(
                pageId = pageId,
                fileName = fileName,
            )
        }
    }
