package ru.kode.android.build.publish.plugin.clickup.task.automation.work

import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.clickup.task.automation.service.ClickUpService

interface AddFixVersionParameters : WorkParameters {
    val apiToken: Property<String>
    val issues: SetProperty<String>
    val version: Property<String>
    val fieldId: Property<String>
}

abstract class AddFixVersionWork : WorkAction<AddFixVersionParameters> {
    private val logger = Logging.getLogger(this::class.java)

    override fun execute() {
        val service =
            ClickUpService(
                logger,
                parameters.apiToken.get(),
            )
        val issues = parameters.issues.get()
        val version = parameters.version.get()
        val fieldId = parameters.fieldId.get()
        issues.forEach { issue ->
            service.addFieldToTask(issue, fieldId, version)
        }
    }
}
