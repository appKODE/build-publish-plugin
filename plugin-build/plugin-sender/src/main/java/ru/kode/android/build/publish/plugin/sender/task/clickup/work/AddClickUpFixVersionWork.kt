package ru.kode.android.build.publish.plugin.sender.task.clickup.work

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.clickup.controller.addFixVersionToTasks
import ru.kode.android.build.publish.plugin.clickup.controller.factory.ClickUpControllerFactory
import javax.inject.Inject

internal interface AddClickUpFixVersionParameters : WorkParameters {
    val apiToken: Property<String>
    val version: Property<String>
    val taskIds: SetProperty<String>
    val workspaceName: Property<String>
    val fieldName: Property<String>
}

internal abstract class AddClickUpFixVersionWork
    @Inject
    constructor() : WorkAction<AddClickUpFixVersionParameters> {
        override fun execute() {
            ClickUpControllerFactory.build(token = parameters.apiToken.get())
                .addFixVersionToTasks(
                    workspaceName = parameters.workspaceName.get(),
                    fieldName = parameters.fieldName.get(),
                    version = parameters.version.get(),
                    taskIds = parameters.taskIds.get(),
                )
        }
    }
