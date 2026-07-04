package ru.kode.android.build.publish.plugin.sender.task.slack

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import ru.kode.android.build.publish.plugin.sender.task.slack.work.SendSlackFileWork
import javax.inject.Inject

@DisableCachingByDefault
abstract class SendSlackFileTask
    @Inject
    constructor(
        private val workerExecutor: WorkerExecutor,
    ) : DefaultTask() {
        init {
            description = "Uploads a file to Slack"
            group = BasePlugin.BUILD_GROUP
        }

        @get:Internal
        abstract val webhookUrl: Property<String>

        @get:Internal
        abstract val uploadApiTokenFile: RegularFileProperty

        @get:InputFile
        @get:PathSensitive(PathSensitivity.RELATIVE)
        @get:Option(option = "file", description = "File to upload")
        abstract val file: RegularFileProperty

        @get:Input
        @get:Option(option = "channels", description = "Slack channels to post to")
        abstract val channels: SetProperty<String>

        @get:Input
        @get:Optional
        @get:Option(option = "comment", description = "Initial comment")
        abstract val comment: Property<String>

        @TaskAction
        fun uploadFile() {
            workerExecutor.noIsolation().submit(SendSlackFileWork::class.java) { params ->
                params.uploadApiToken.set(uploadApiTokenFile.map { it.asFile.readText().trim() })
                params.file.set(file)
                params.channels.set(channels)
                params.comment.set(comment.orElse(""))
            }
        }
    }
