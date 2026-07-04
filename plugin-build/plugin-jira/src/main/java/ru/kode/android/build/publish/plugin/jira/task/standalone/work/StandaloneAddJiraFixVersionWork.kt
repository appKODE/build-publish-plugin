package ru.kode.android.build.publish.plugin.jira.task.standalone.work

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import ru.kode.android.build.publish.plugin.core.task.ServiceWorkParameters
import ru.kode.android.build.publish.plugin.jira.service.network.JiraService
import javax.inject.Inject

internal interface StandaloneAddJiraFixVersionParameters : ServiceWorkParameters {
    val instanceName: Property<String>
    val version: Property<String>
    val projectKey: Property<String>
    val issues: SetProperty<String>
    val service: Property<JiraService>
}

internal abstract class StandaloneAddJiraFixVersionWork
    @Inject
    constructor() : WorkAction<StandaloneAddJiraFixVersionParameters> {
        override fun execute() {
            val logger = parameters.loggerService.get()
            parameters.service.get().addFixVersionToIssues(
                instanceName = parameters.instanceName.getOrElse(""),
                projectKey = parameters.projectKey.get(),
                version = parameters.version.get(),
                issues = parameters.issues.get(),
                log = logger::info,
            )
        }
    }
