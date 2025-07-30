package ru.kode.android.build.publish.plugin.clickup.task.automation.work

import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.clickup.task.automation.service.ClickUpService

interface AddTagToTaskParameters : WorkParameters {
    val apiToken: Property<String>
    val tagName: Property<String>
    val issues: SetProperty<String>
}

abstract class AddTagToTaskWork : WorkAction<AddTagToTaskParameters> {
    private val logger = Logging.getLogger(this::class.java)

    override fun execute() {
        val service =
            ClickUpService(
                logger,
                parameters.apiToken.get(),
            )
        val issues = parameters.issues.get()
        val tagName = parameters.tagName.get()
        issues.forEach { issue -> service.addTagToTask(issue, tagName) }
    }
}
