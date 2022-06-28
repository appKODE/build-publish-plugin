package ru.kode.android.build.publish.plugin.task.jira.work

import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.task.jira.service.JiraService

interface UpdateJiraLabelsParameters : WorkParameters {
    val baseUrl: Property<String>
    val username: Property<String>
    val password: Property<String>
    val issues: SetProperty<String>
    val label: Property<String>
}

abstract class AddLabelWork : WorkAction<UpdateJiraLabelsParameters> {

    private val logger = Logging.getLogger(this::class.java)

    @Suppress("SwallowedException") // see logs below
    override fun execute() {
        val service = JiraService(
            logger,
            parameters.baseUrl.get(),
            parameters.username.get(),
            parameters.password.get()
        )
        val issues = parameters.issues.get()
        issues.forEach { issue -> service.addLabel(issue, parameters.label.get()) }
    }
}
