package ru.kode.android.build.publish.plugin.jira.task.standalone.work

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import ru.kode.android.build.publish.plugin.core.task.ServiceWorkParameters
import ru.kode.android.build.publish.plugin.jira.service.network.JiraService
import javax.inject.Inject

internal interface StandaloneTransitionJiraIssueParameters : ServiceWorkParameters {
    val transitionName: Property<String>
    val projectKey: Property<String>
    val issues: SetProperty<String>
    val service: Property<JiraService>
}

internal abstract class StandaloneTransitionJiraIssueWork
    @Inject
    constructor() : WorkAction<StandaloneTransitionJiraIssueParameters> {
        override fun execute() {
            parameters.service.get().transitionIssues(
                projectKey = parameters.projectKey.get(),
                transitionName = parameters.transitionName.get(),
                issues = parameters.issues.get(),
                log = parameters.loggerService.get()::info,
            )
        }
    }
