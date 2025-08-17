package ru.kode.android.build.publish.plugin.clickup.task.automation.work

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.clickup.service.network.ClickUpNetworkService

/**
 * Parameters for the [AddTagToTaskWork] work action.
 *
 * This interface defines the input parameters required to add a tag to ClickUp tasks.
 * It's used by Gradle's worker API to pass data to the work action.
 */
internal interface AddTagToTaskParameters : WorkParameters {
    /**
     * The name of the tag to add to the tasks
     */
    val tagName: Property<String>

    /**
     * The set of ClickUp task IDs to add the tag to
     */
    val issues: SetProperty<String>

    /**
     * The network service used to communicate with the ClickUp API
     */
    val networkService: Property<ClickUpNetworkService>
}

/**
 * A Gradle work action that adds a tag to multiple ClickUp tasks.
 *
 * This work action is executed asynchronously by Gradle's worker API. It takes a set of
 * ClickUp task IDs and adds the specified tag to each one using the provided network service.
 *
 * The work is performed in a background thread, making it suitable for network operations
 * that might take a significant amount of time.
 *
 * @see WorkAction For more information about Gradle work actions
 * @see ClickUpNetworkService For the underlying network operations
 */
internal abstract class AddTagToTaskWork : WorkAction<AddTagToTaskParameters> {
    override fun execute() {
        val service = parameters.networkService.get()
        val issues = parameters.issues.get()
        val tagName = parameters.tagName.get()

        issues.forEach { issue ->
            service.addTagToTask(issue, tagName)
        }
    }
}
