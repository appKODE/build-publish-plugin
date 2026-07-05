package ru.kode.android.build.publish.plugin.clickup.task.standalone.work

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import ru.kode.android.build.publish.plugin.clickup.service.network.ClickUpService
import ru.kode.android.build.publish.plugin.core.task.ServiceWorkParameters
import javax.inject.Inject

internal interface StandaloneAddClickUpFixVersionParameters : ServiceWorkParameters {
    val accountName: Property<String>
    val version: Property<String>
    val taskIds: SetProperty<String>
    val workspaceName: Property<String>
    val fieldName: Property<String>
    val service: Property<ClickUpService>
}

internal abstract class StandaloneAddClickUpFixVersionWork
    @Inject
    constructor() : WorkAction<StandaloneAddClickUpFixVersionParameters> {
        override fun execute() {
            parameters.service.get().addFixVersionToTasks(
                accountName = parameters.accountName.getOrElse(""),
                workspaceName = parameters.workspaceName.get(),
                fieldName = parameters.fieldName.get(),
                version = parameters.version.get(),
                taskIds = parameters.taskIds.get(),
                log = parameters.loggerService.get()::info,
            )
        }
    }
