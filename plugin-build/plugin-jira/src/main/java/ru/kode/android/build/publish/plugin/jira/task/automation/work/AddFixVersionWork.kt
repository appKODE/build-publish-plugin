package ru.kode.android.build.publish.plugin.jira.task.automation.work

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.jira.service.network.JiraService

/**
 * Parameters for the [AddFixVersionWork] work action.
 *
 * This interface defines the input parameters required to add fix versions to Jira issues.
 * It's used to pass data to the [AddFixVersionWork] work action.
 */
internal interface AddFixVersionParameters : WorkParameters {
    /**
     * The numeric ID of the Jira project where the version will be created
     */
    val projectId: Property<Long>

    /**
     * The set of Jira issue keys to update (e.g., ["PROJ-123", "PROJ-456"])
     */
    val issues: SetProperty<String>

    /**
     * The version string to add as a fix version (e.g., "1.2.3")
     */
    val version: Property<String>

    /**
     * The network service used to communicate with the Jira API
     */
    val service: Property<JiraService>
}

/**
 * A Gradle work action that creates a Jira version and adds it as a fix version to multiple issues.
 *
 * This work action performs the following operations in sequence:
 * 1. Creates a new version in the specified Jira project
 * 2. Adds this version as a fix version to each of the specified issues
 *
 * Note: This work action will fail if either the version creation or any of the
 * fix version additions fail. All operations are performed sequentially, and the first
 * failure will stop further processing.
 *
 * @see [JiraService.createVersion] for version creation implementation
 * @see [JiraService.addFixVersion] for adding fix version implementation
 */
internal abstract class AddFixVersionWork : WorkAction<AddFixVersionParameters> {
    override fun execute() {
        val service = parameters.service.get()
        val issues = parameters.issues.get()
        val version = parameters.version.get()
        val projectId = parameters.projectId.get()

        service.createVersion(projectId, version)
        issues.forEach { issue ->
            service.addFixVersion(issue, version)
        }
    }
}
