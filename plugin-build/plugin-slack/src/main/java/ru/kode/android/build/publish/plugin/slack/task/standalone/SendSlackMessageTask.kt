package ru.kode.android.build.publish.plugin.slack.task.standalone

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import ru.kode.android.build.publish.plugin.core.task.StandaloneServiceTask
import ru.kode.android.build.publish.plugin.slack.service.SlackService
import ru.kode.android.build.publish.plugin.slack.task.standalone.work.SendSlackMessageWork
import javax.inject.Inject

@DisableCachingByDefault
abstract class SendSlackMessageTask
    @Inject
    constructor(
        workerExecutor: WorkerExecutor,
    ) : StandaloneServiceTask<SlackService>(workerExecutor) {
        init {
            description = "Sends a message to Slack"
        }

        @get:Input
        @get:Option(option = "message", description = "Message text to send")
        abstract val message: Property<String>

        @get:Input
        @get:Optional
        @get:Option(option = "attachmentColor", description = "Attachment vertical line color in hex, e.g. #36a64f")
        abstract val attachmentColor: Property<String>

        @get:Input
        @get:Optional
        @get:Option(option = "iconUrl", description = "Icon URL to show in chat")
        abstract val iconUrl: Property<String>

        @TaskAction
        fun sendMessage() {
            val workQueue = workerExecutor.noIsolation()
            workQueue.submit(SendSlackMessageWork::class.java) { params ->
                params.message.set(message)
                params.attachmentColor.set(attachmentColor.orElse("#36a64f"))
                params.iconUrl.set(iconUrl.orElse(""))
                params.service.set(service)
                params.loggerService.set(loggerService)
            }
        }
    }
