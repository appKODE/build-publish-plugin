package ru.kode.android.build.publish.plugin.clickup.task.standalone

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import ru.kode.android.build.publish.plugin.clickup.service.network.ClickUpService
import ru.kode.android.build.publish.plugin.clickup.task.standalone.work.StandaloneAddClickUpTagWork
import ru.kode.android.build.publish.plugin.core.task.StandaloneServiceTask
import javax.inject.Inject

@DisableCachingByDefault
abstract class AddClickUpTagTask
    @Inject
    constructor(
        workerExecutor: WorkerExecutor,
    ) : StandaloneServiceTask<ClickUpService>(workerExecutor) {
        init {
            description = "Adds a tag to ClickUp tasks"
        }

        @get:Input
        @get:Option(option = "tag", description = "Tag name to add to the tasks")
        abstract val tag: Property<String>

        @get:Input
        @get:Option(option = "taskIds", description = "ClickUp task IDs to tag")
        abstract val taskIds: SetProperty<String>

        @TaskAction
        fun addTag() {
            val workQueue = workerExecutor.noIsolation()
            workQueue.submit(StandaloneAddClickUpTagWork::class.java) { params ->
                params.tag.set(tag)
                params.taskIds.set(taskIds)
                params.service.set(service)
                params.loggerService.set(loggerService)
            }
        }
    }
