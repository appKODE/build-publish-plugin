package ru.kode.android.build.publish.plugin.task.jira.work

import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.task.jira.service.JiraService

interface AddFixVersionParameters : WorkParameters {
    val baseUrl: Property<String>
    val projectId: Property<Long>
    val username: Property<String>
    val password: Property<String>
    val issues: SetProperty<String>
    val version: Property<String>
}

abstract class AddFixVersionWork : WorkAction<AddFixVersionParameters> {

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
        val version = parameters.version.get()
        val projectId = parameters.projectId.get()
        service.createVersion(projectId, version)
        issues.forEach { issue -> service.addFixVersion(issue, version) }
    }
}
