package ru.kode.android.build.publish.plugin.sender.task.confluence

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import ru.kode.android.build.publish.plugin.sender.task.base.BaseBasicAuthSenderTask
import ru.kode.android.build.publish.plugin.sender.task.confluence.work.AddConfluenceCommentWork
import javax.inject.Inject

@DisableCachingByDefault
abstract class AddConfluenceCommentTask
    @Inject
    constructor(
        workerExecutor: WorkerExecutor,
    ) : BaseBasicAuthSenderTask(workerExecutor) {
        init {
            description = "Adds a comment with a file download link to a Confluence page"
        }

        @get:Input
        @get:Option(option = "pageId", description = "Confluence page ID to add the comment to")
        abstract val pageId: Property<String>

        @get:Input
        @get:Option(option = "fileName", description = "Name of the attached file to link in the comment")
        abstract val fileName: Property<String>

        @TaskAction
        fun addComment() {
            workerExecutor.noIsolation().submit(AddConfluenceCommentWork::class.java) { params ->
                params.baseUrl.set(baseUrl)
                params.username.set(username)
                params.apiToken.set(apiToken)
                params.pageId.set(pageId)
                params.fileName.set(fileName)
            }
        }
    }
