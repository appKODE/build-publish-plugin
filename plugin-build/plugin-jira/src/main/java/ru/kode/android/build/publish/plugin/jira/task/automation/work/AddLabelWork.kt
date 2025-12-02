package ru.kode.android.build.publish.plugin.jira.task.automation.work

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.jira.service.network.JiraService

/**
 * Parameters for the [AddLabelWork] work action.
 *
 * This interface defines the input parameters required to add labels to Jira issues.
 * It's used to pass data to the [AddLabelWork] work action.
 */
internal interface AddLabelParameters : WorkParameters {
    /**
     * The set of Jira issue keys to label (e.g., ["PROJ-123", "PROJ-456"])
     */
    val issues: SetProperty<String>

    /**
     * The label to add to the specified issues
     */
    val label: Property<String>

    /**
     * The service used to communicate with the Jira
     */
    val service: Property<JiraService>
}

/**
 * A Gradle work action that adds a label to multiple Jira issues.
 *
 * This work action is responsible for:
 * 1. Taking a set of Jira issue keys and a label to add
 * 2. Adding the specified label to each issue using the provided [JiraService]
 *
 * Note: This work action will fail if any of the label additions fail. All operations are
 * performed sequentially, and the first failure will stop further processing.
 *
 * @see [JiraService.addLabel] for the actual API call implementation
 */
internal abstract class AddLabelWork : WorkAction<AddLabelParameters> {
    override fun execute() {
        val service = parameters.service.get()
        val issues = parameters.issues.get()
        val label = parameters.label.get()

        issues.forEach { issue ->
            service.addLabel(issue, label)
        }
    }
}
