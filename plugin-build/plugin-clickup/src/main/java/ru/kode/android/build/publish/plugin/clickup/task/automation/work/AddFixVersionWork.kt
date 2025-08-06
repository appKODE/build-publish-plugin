package ru.kode.android.build.publish.plugin.clickup.task.automation.work

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.clickup.service.ClickUpNetworkService

internal interface AddFixVersionParameters : WorkParameters {
    val issues: SetProperty<String>
    val version: Property<String>
    val fieldId: Property<String>
    val networkService: Property<ClickUpNetworkService>
}

internal abstract class AddFixVersionWork : WorkAction<AddFixVersionParameters> {
    override fun execute() {
        val service = parameters.networkService.get()
        val issues = parameters.issues.get()
        val version = parameters.version.get()
        val fieldId = parameters.fieldId.get()
        issues.forEach { issue ->
            service.addFieldToTask(issue, fieldId, version)
        }
    }
}
