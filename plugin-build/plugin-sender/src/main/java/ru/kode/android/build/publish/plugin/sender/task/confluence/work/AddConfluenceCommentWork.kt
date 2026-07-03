package ru.kode.android.build.publish.plugin.sender.task.confluence.work

import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.confluence.controller.factory.ConfluenceControllerFactory
import javax.inject.Inject

internal interface AddConfluenceCommentParameters : WorkParameters {
    val baseUrl: Property<String>
    val username: Property<String>
    val apiToken: Property<String>
    val pageId: Property<String>
    val fileName: Property<String>
}

internal abstract class AddConfluenceCommentWork
    @Inject
    constructor() : WorkAction<AddConfluenceCommentParameters> {
        override fun execute() {
            ConfluenceControllerFactory.build(
                baseUrl = parameters.baseUrl.get(),
                username = parameters.username.get(),
                password = parameters.apiToken.get(),
            ).addComment(
                pageId = parameters.pageId.get(),
                fileName = parameters.fileName.get(),
            )
        }
    }
