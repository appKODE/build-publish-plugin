package ru.kode.android.build.publish.plugin.sender.task.clickup.work

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.clickup.controller.addTagToTasks
import ru.kode.android.build.publish.plugin.clickup.controller.factory.ClickUpControllerFactory
import javax.inject.Inject

internal interface AddClickUpTagParameters : WorkParameters {
    val apiToken: Property<String>
    val tag: Property<String>
    val taskIds: SetProperty<String>
}

internal abstract class AddClickUpTagWork
    @Inject
    constructor() : WorkAction<AddClickUpTagParameters> {
        override fun execute() {
            ClickUpControllerFactory.build(token = parameters.apiToken.get())
                .addTagToTasks(
                    tagName = parameters.tag.get(),
                    taskIds = parameters.taskIds.get(),
                )
        }
    }
