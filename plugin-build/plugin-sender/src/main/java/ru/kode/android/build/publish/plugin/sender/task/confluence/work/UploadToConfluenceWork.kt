package ru.kode.android.build.publish.plugin.sender.task.confluence.work

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.confluence.controller.factory.ConfluenceControllerFactory
import javax.inject.Inject

internal interface UploadToConfluenceParameters : WorkParameters {
    val baseUrl: Property<String>
    val username: Property<String>
    val apiToken: Property<String>
    val file: RegularFileProperty
    val pageId: Property<String>
}

internal abstract class UploadToConfluenceWork
    @Inject
    constructor() : WorkAction<UploadToConfluenceParameters> {
        override fun execute() {
            ConfluenceControllerFactory.build(
                baseUrl = parameters.baseUrl.get(),
                username = parameters.username.get(),
                password = parameters.apiToken.get(),
            ).uploadFile(
                pageId = parameters.pageId.get(),
                file = parameters.file.asFile.get(),
            )
        }
    }
