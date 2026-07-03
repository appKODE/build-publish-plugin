package ru.kode.android.build.publish.plugin.sender.task.jira.work

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.jira.controller.addFixVersionToIssues
import ru.kode.android.build.publish.plugin.jira.controller.factory.JiraControllerFactory
import javax.inject.Inject

internal interface AddJiraFixVersionParameters : WorkParameters {
    val baseUrl: Property<String>
    val username: Property<String>
    val apiToken: Property<String>
    val version: Property<String>
    val projectKey: Property<String>
    val issues: SetProperty<String>
}

internal abstract class AddJiraFixVersionWork
    @Inject
    constructor() : WorkAction<AddJiraFixVersionParameters> {
        override fun execute() {
            JiraControllerFactory.build(
                baseUrl = parameters.baseUrl.get(),
                username = parameters.username.get(),
                password = parameters.apiToken.get(),
            ).addFixVersionToIssues(
                projectKey = parameters.projectKey.get(),
                version = parameters.version.get(),
                issues = parameters.issues.get(),
            )
        }
    }
