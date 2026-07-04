package ru.kode.android.build.publish.plugin.sender.task.confluence

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import ru.kode.android.build.publish.plugin.sender.task.base.BaseBasicAuthSenderTask
import ru.kode.android.build.publish.plugin.sender.task.confluence.work.UploadToConfluenceWork
import javax.inject.Inject

@DisableCachingByDefault
abstract class UploadToConfluenceTask
    @Inject
    constructor(
        workerExecutor: WorkerExecutor,
    ) : BaseBasicAuthSenderTask(workerExecutor) {
        init {
            description = "Uploads a file to a Confluence page"
        }

        @get:InputFile
        @get:PathSensitive(PathSensitivity.RELATIVE)
        @get:Option(option = "file", description = "File to upload")
        abstract val file: RegularFileProperty

        @get:Input
        @get:Option(option = "pageId", description = "Confluence page ID to attach the file to")
        abstract val pageId: Property<String>

        @TaskAction
        fun uploadFile() {
            workerExecutor.noIsolation().submit(UploadToConfluenceWork::class.java) { params ->
                params.baseUrl.set(baseUrl)
                params.username.set(username)
                params.apiToken.set(apiToken)
                params.file.set(file)
                params.pageId.set(pageId)
            }
        }
    }
