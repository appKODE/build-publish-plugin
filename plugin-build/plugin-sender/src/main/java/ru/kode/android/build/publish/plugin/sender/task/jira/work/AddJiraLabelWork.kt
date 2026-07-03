package ru.kode.android.build.publish.plugin.sender.task.jira.work

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.jira.controller.addLabelToIssues
import ru.kode.android.build.publish.plugin.jira.controller.factory.JiraControllerFactory
import javax.inject.Inject

internal interface AddJiraLabelParameters : WorkParameters {
    val baseUrl: Property<String>
    val username: Property<String>
    val apiToken: Property<String>
    val label: Property<String>
    val issues: SetProperty<String>
}

internal abstract class AddJiraLabelWork
    @Inject
    constructor() : WorkAction<AddJiraLabelParameters> {
        override fun execute() {
            JiraControllerFactory.build(
                baseUrl = parameters.baseUrl.get(),
                username = parameters.username.get(),
                password = parameters.apiToken.get(),
            ).addLabelToIssues(
                label = parameters.label.get(),
                issues = parameters.issues.get(),
            )
        }
    }
