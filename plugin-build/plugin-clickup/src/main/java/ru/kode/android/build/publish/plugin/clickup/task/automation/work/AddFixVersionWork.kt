package ru.kode.android.build.publish.plugin.clickup.task.automation.work

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.clickup.service.network.ClickUpNetworkService

/**
 * Parameters for the [AddFixVersionWork] work action.
 *
 * This interface defines the input parameters required to update the fix version
 * for ClickUp tasks. It's used by Gradle's worker API to pass data to the work action.
 */
internal interface AddFixVersionParameters : WorkParameters {
    /**
     *  issues The set of ClickUp task IDs to update
     */
    val issues: SetProperty<String>

    /**
     * The version string to set for the tasks
     */
    val version: Property<String>

    /**
     * The ID of the custom field that stores the fix version
     */
    val fieldId: Property<String>

    /**
     * The network service used to communicate with the ClickUp API
     */
    val networkService: Property<ClickUpNetworkService>
}

/**
 * A Gradle work action that updates the fix version for multiple ClickUp tasks.
 *
 * This work action is executed asynchronously by Gradle's worker API. It takes a set of
 * ClickUp task IDs and updates the specified custom field with the provided version
 * string for each task.
 *
 * The work is performed in a background thread, making it suitable for network operations
 * that might take a significant amount of time.
 *
 * @see WorkAction For more information about Gradle work actions
 * @see ClickUpNetworkService For the underlying network operations
 */
internal abstract class AddFixVersionWork : WorkAction<AddFixVersionParameters> {
    override fun execute() {
        val service = parameters.networkService.get()
        val issues = parameters.issues.get()
        val version = parameters.version.get()
        val fieldId = parameters.fieldId.get()
        issues.forEach { issue ->
            service.addFieldToTask(issue, fieldId, version)
        }
    }
}
