package ru.kode.android.build.publish.plugin.sender.task.clickup

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import ru.kode.android.build.publish.plugin.sender.task.base.BaseClickUpSenderTask
import ru.kode.android.build.publish.plugin.sender.task.clickup.work.AddClickUpFixVersionWork
import javax.inject.Inject

@DisableCachingByDefault
abstract class AddClickUpFixVersionTask
    @Inject
    constructor(
        workerExecutor: WorkerExecutor,
    ) : BaseClickUpSenderTask(workerExecutor) {
        init {
            description = "Adds a fix version custom field to ClickUp tasks"
        }

        @get:Input
        @get:Option(option = "version", description = "Version value to set on the custom field")
        abstract val version: Property<String>

        @get:Input
        @get:Option(option = "taskIds", description = "ClickUp task IDs to update")
        abstract val taskIds: SetProperty<String>

        @get:Input
        @get:Option(option = "workspaceName", description = "ClickUp workspace name")
        abstract val workspaceName: Property<String>

        @get:Input
        @get:Option(option = "fieldName", description = "Custom field name to set the version on")
        abstract val fieldName: Property<String>

        @TaskAction
        fun addFixVersion() {
            workerExecutor.noIsolation().submit(AddClickUpFixVersionWork::class.java) { params ->
                params.apiToken.set(apiTokenFile.map { it.asFile.readText().trim() })
                params.version.set(version)
                params.taskIds.set(taskIds)
                params.workspaceName.set(workspaceName)
                params.fieldName.set(fieldName)
            }
        }
    }
