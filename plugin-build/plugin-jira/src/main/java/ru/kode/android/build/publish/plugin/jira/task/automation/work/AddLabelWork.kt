package ru.kode.android.build.publish.plugin.jira.task.automation.work

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.jira.service.JiraNetworkService

interface AddLabelParameters : WorkParameters {
    val issues: SetProperty<String>
    val label: Property<String>
    val networkService: Property<JiraNetworkService>
}

internal abstract class AddLabelWork : WorkAction<AddLabelParameters> {
    override fun execute() {
        val service = parameters.networkService.get()
        val issues = parameters.issues.get()
        issues.forEach { issue -> service.addLabel(issue, parameters.label.get()) }
    }
}
