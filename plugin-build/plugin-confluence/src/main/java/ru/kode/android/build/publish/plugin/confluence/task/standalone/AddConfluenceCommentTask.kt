package ru.kode.android.build.publish.plugin.confluence.task.standalone

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import ru.kode.android.build.publish.plugin.confluence.service.ConfluenceService
import ru.kode.android.build.publish.plugin.confluence.task.standalone.work.StandaloneAddConfluenceCommentWork
import ru.kode.android.build.publish.plugin.core.task.StandaloneServiceTask
import javax.inject.Inject

@DisableCachingByDefault
abstract class AddConfluenceCommentTask
    @Inject
    constructor(
        workerExecutor: WorkerExecutor,
    ) : StandaloneServiceTask<ConfluenceService>(workerExecutor) {
        init {
            description = "Adds a comment with a file download link to a Confluence page"
        }

        @get:Input
        @get:Option(option = "pageId", description = "Confluence page ID to add the comment to")
        abstract val pageId: Property<String>

        @get:Input
        @get:Option(option = "fileName", description = "Name of the file to create a download link for")
        abstract val fileName: Property<String>

        @TaskAction
        fun addComment() {
            val workQueue = workerExecutor.noIsolation()
            workQueue.submit(StandaloneAddConfluenceCommentWork::class.java) { params ->
                params.pageId.set(pageId)
                params.fileName.set(fileName)
                params.service.set(service)
                params.loggerService.set(loggerService)
            }
        }
    }
