package ru.kode.android.build.publish.plugin.clickup.task.standalone.work

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import ru.kode.android.build.publish.plugin.clickup.service.network.ClickUpService
import ru.kode.android.build.publish.plugin.core.task.ServiceWorkParameters
import javax.inject.Inject

internal interface StandaloneAddClickUpTagParameters : ServiceWorkParameters {
    val accountName: Property<String>
    val tag: Property<String>
    val taskIds: SetProperty<String>
    val service: Property<ClickUpService>
}

internal abstract class StandaloneAddClickUpTagWork
    @Inject
    constructor() : WorkAction<StandaloneAddClickUpTagParameters> {
        override fun execute() {
            parameters.service.get().addTagToTasks(
                accountName = parameters.accountName.getOrElse(""),
                tagName = parameters.tag.get(),
                taskIds = parameters.taskIds.get(),
                log = parameters.loggerService.get()::info,
            )
        }
    }
