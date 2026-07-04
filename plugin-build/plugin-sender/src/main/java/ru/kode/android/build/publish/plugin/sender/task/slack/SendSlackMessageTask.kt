package ru.kode.android.build.publish.plugin.sender.task.slack

import org.gradle.api.DefaultTask
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import ru.kode.android.build.publish.plugin.sender.task.slack.work.SendSlackMessageWork
import javax.inject.Inject

@DisableCachingByDefault
abstract class SendSlackMessageTask
    @Inject
    constructor(
        private val workerExecutor: WorkerExecutor,
    ) : DefaultTask() {
        init {
            description = "Sends a message to Slack"
            group = BasePlugin.BUILD_GROUP
        }

        @get:Internal
        abstract val webhookUrl: Property<String>

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
            workerExecutor.noIsolation().submit(SendSlackMessageWork::class.java) { params ->
                params.webhookUrl.set(webhookUrl)
                params.message.set(message)
                params.attachmentColor.set(attachmentColor.orElse("#36a64f"))
                params.iconUrl.set(iconUrl.orElse(""))
            }
        }
    }
