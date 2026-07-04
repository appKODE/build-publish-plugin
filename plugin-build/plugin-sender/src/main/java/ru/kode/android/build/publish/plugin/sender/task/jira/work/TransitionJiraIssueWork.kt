package ru.kode.android.build.publish.plugin.sender.task.jira.work

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.jira.controller.factory.JiraControllerFactory
import ru.kode.android.build.publish.plugin.jira.controller.transitionIssues
import javax.inject.Inject

internal interface TransitionJiraIssueParameters : WorkParameters {
    val baseUrl: Property<String>
    val username: Property<String>
    val apiToken: Property<String>
    val transitionName: Property<String>
    val projectKey: Property<String>
    val issues: SetProperty<String>
}

internal abstract class TransitionJiraIssueWork
    @Inject
    constructor() : WorkAction<TransitionJiraIssueParameters> {
        override fun execute() {
            JiraControllerFactory.build(
                baseUrl = parameters.baseUrl.get(),
                username = parameters.username.get(),
                password = parameters.apiToken.get(),
            ).transitionIssues(
                projectKey = parameters.projectKey.get(),
                transitionName = parameters.transitionName.get(),
                issues = parameters.issues.get(),
            )
        }
    }
