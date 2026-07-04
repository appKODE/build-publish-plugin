package ru.kode.android.build.publish.plugin.jira.task.standalone.work

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import ru.kode.android.build.publish.plugin.core.task.ServiceWorkParameters
import ru.kode.android.build.publish.plugin.jira.service.network.JiraService
import javax.inject.Inject

internal interface StandaloneAddJiraLabelParameters : ServiceWorkParameters {
    val instanceName: Property<String>
    val label: Property<String>
    val issues: SetProperty<String>
    val service: Property<JiraService>
}

internal abstract class StandaloneAddJiraLabelWork
    @Inject
    constructor() : WorkAction<StandaloneAddJiraLabelParameters> {
        override fun execute() {
            parameters.service.get().addLabelToIssues(
                instanceName = parameters.instanceName.getOrElse(""),
                label = parameters.label.get(),
                issues = parameters.issues.get(),
                log = parameters.loggerService.get()::info,
            )
        }
    }
