package ru.kode.android.build.publish.plugin.clickup.task.standalone

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import ru.kode.android.build.publish.plugin.clickup.service.network.ClickUpService
import ru.kode.android.build.publish.plugin.clickup.task.standalone.work.StandaloneAddClickUpFixVersionWork
import ru.kode.android.build.publish.plugin.core.task.StandaloneServiceTask
import javax.inject.Inject

@DisableCachingByDefault
abstract class AddClickUpFixVersionTask
    @Inject
    constructor(
        workerExecutor: WorkerExecutor,
    ) : StandaloneServiceTask<ClickUpService>(workerExecutor) {
        init {
            description = "Adds a fix version to ClickUp tasks via a custom field"
        }

        @get:Input
        @get:Option(option = "version", description = "Version string to set as the fix version")
        abstract val version: Property<String>

        @get:Input
        @get:Option(option = "taskIds", description = "ClickUp task IDs to update")
        abstract val taskIds: SetProperty<String>

        @get:Input
        @get:Option(option = "workspaceName", description = "ClickUp workspace name")
        abstract val workspaceName: Property<String>

        @get:Input
        @get:Option(option = "fieldName", description = "Custom field name that stores the fix version")
        abstract val fieldName: Property<String>

        @get:Input
        @get:Optional
        @get:Option(option = "accountName", description = "Name of the ClickUp account to use")
        abstract val accountName: Property<String>

        @TaskAction
        fun addFixVersion() {
            val workQueue = workerExecutor.noIsolation()
            workQueue.submit(StandaloneAddClickUpFixVersionWork::class.java) { params ->
                params.accountName.set(accountName)
                params.version.set(version)
                params.taskIds.set(taskIds)
                params.workspaceName.set(workspaceName)
                params.fieldName.set(fieldName)
                params.service.set(service)
                params.loggerService.set(loggerService)
            }
        }
    }
