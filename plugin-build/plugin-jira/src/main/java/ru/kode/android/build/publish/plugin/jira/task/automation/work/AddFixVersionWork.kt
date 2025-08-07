package ru.kode.android.build.publish.plugin.jira.task.automation.work

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.jira.service.network.JiraNetworkService

internal interface AddFixVersionParameters : WorkParameters {
    val projectId: Property<Long>
    val issues: SetProperty<String>
    val version: Property<String>
    val networkService: Property<JiraNetworkService>
}

internal abstract class AddFixVersionWork : WorkAction<AddFixVersionParameters> {
    @Suppress("SwallowedException") // see logs below
    override fun execute() {
        val service = parameters.networkService.get()
        val issues = parameters.issues.get()
        val version = parameters.version.get()
        val projectId = parameters.projectId.get()
        service.createVersion(projectId, version)
        issues.forEach { issue -> service.addFixVersion(issue, version) }
    }
}
