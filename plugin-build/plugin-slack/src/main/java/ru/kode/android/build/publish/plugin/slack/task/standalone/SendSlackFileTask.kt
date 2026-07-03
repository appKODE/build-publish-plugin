package ru.kode.android.build.publish.plugin.slack.task.standalone

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import ru.kode.android.build.publish.plugin.core.task.StandaloneServiceTask
import ru.kode.android.build.publish.plugin.slack.service.SlackService
import ru.kode.android.build.publish.plugin.slack.task.standalone.work.SendSlackFileWork
import javax.inject.Inject

@DisableCachingByDefault
abstract class SendSlackFileTask
    @Inject
    constructor(
        workerExecutor: WorkerExecutor,
    ) : StandaloneServiceTask<SlackService>(workerExecutor) {
        init {
            description = "Uploads a file to Slack"
        }

        @get:InputFile
        @get:Option(option = "file", description = "File to upload (absolute path)")
        @get:PathSensitive(PathSensitivity.RELATIVE)
        abstract val file: RegularFileProperty

        @get:Input
        @get:Option(option = "channels", description = "Slack channels to post the file to")
        abstract val channels: SetProperty<String>

        @get:Input
        @get:Optional
        @get:Option(option = "comment", description = "Initial comment for the file upload")
        abstract val comment: Property<String>

        @TaskAction
        fun uploadFile() {
            val workQueue = workerExecutor.noIsolation()
            workQueue.submit(SendSlackFileWork::class.java) { params ->
                params.file.set(file)
                params.channels.set(channels)
                params.comment.set(comment.orElse(""))
                params.service.set(service)
                params.loggerService.set(loggerService)
            }
        }
    }
