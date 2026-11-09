package ru.kode.android.build.publish.plugin.vk.task.standalone

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
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
import ru.kode.android.build.publish.plugin.vk.service.VkService
import ru.kode.android.build.publish.plugin.vk.task.standalone.work.UploadToVkWork
import javax.inject.Inject

@DisableCachingByDefault
abstract class UploadToVkTask
    @Inject
    constructor(
        workerExecutor: WorkerExecutor,
    ) : StandaloneServiceTask<VkService>(workerExecutor) {
        init {
            description = "Sends a file as a document to a VK conversation"
        }

        @get:InputFile
        @get:Option(option = "file", description = "File to send (absolute path)")
        @get:PathSensitive(PathSensitivity.RELATIVE)
        abstract val file: RegularFileProperty

        @get:Input
        @get:Option(option = "peerId", description = "VK conversation id: user id or 2000000000 + chat_id")
        abstract val peerId: Property<Long>

        @get:Input
        @get:Optional
        @get:Option(option = "title", description = "Document title (defaults to local file name)")
        abstract val title: Property<String>

        @get:Input
        @get:Optional
        @get:Option(option = "message", description = "Optional text sent with the document")
        abstract val message: Property<String>

        @TaskAction
        fun upload() {
            val resolvedTitle = title.orNull ?: file.asFile.get().name
            val workQueue = workerExecutor.noIsolation()
            workQueue.submit(UploadToVkWork::class.java) { params ->
                params.file.set(file)
                params.peerId.set(peerId)
                params.title.set(resolvedTitle)
                params.message.set(message)
                params.service.set(service)
                params.loggerService.set(loggerService)
            }
        }
    }
