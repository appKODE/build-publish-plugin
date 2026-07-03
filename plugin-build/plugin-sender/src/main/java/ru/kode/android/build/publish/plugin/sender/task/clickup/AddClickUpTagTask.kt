package ru.kode.android.build.publish.plugin.sender.task.clickup

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import ru.kode.android.build.publish.plugin.sender.task.base.BaseClickUpSenderTask
import ru.kode.android.build.publish.plugin.sender.task.clickup.work.AddClickUpTagWork
import javax.inject.Inject

@DisableCachingByDefault
abstract class AddClickUpTagTask
    @Inject
    constructor(
        workerExecutor: WorkerExecutor,
    ) : BaseClickUpSenderTask(workerExecutor) {
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
            workerExecutor.noIsolation().submit(AddClickUpTagWork::class.java) { params ->
                params.apiToken.set(apiTokenFile.map { it.asFile.readText().trim() })
                params.tag.set(tag)
                params.taskIds.set(taskIds)
            }
        }
    }
