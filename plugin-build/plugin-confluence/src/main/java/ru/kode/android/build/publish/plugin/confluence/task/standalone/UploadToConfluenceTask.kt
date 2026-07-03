package ru.kode.android.build.publish.plugin.confluence.task.standalone

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
import ru.kode.android.build.publish.plugin.confluence.service.ConfluenceService
import ru.kode.android.build.publish.plugin.confluence.task.standalone.work.StandaloneUploadToConfluenceWork
import ru.kode.android.build.publish.plugin.core.task.StandaloneServiceTask
import javax.inject.Inject

@DisableCachingByDefault
abstract class UploadToConfluenceTask
    @Inject
    constructor(
        workerExecutor: WorkerExecutor,
    ) : StandaloneServiceTask<ConfluenceService>(workerExecutor) {
        init {
            description = "Uploads a file to a Confluence page"
        }

        @get:InputFile
        @get:Option(option = "file", description = "File to upload (absolute path)")
        @get:PathSensitive(PathSensitivity.RELATIVE)
        abstract val file: RegularFileProperty

        @get:Input
        @get:Option(option = "pageId", description = "Confluence page ID to attach the file to")
        abstract val pageId: Property<String>

        @TaskAction
        fun upload() {
            val workQueue = workerExecutor.noIsolation()
            workQueue.submit(StandaloneUploadToConfluenceWork::class.java) { params ->
                params.file.set(file)
                params.pageId.set(pageId)
                params.service.set(service)
                params.loggerService.set(loggerService)
            }
        }
    }
