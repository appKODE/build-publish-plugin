package ru.kode.android.build.publish.plugin.jira.task.automation.work

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.core.logger.LoggerService
import ru.kode.android.build.publish.plugin.core.util.UploadError
import ru.kode.android.build.publish.plugin.jira.messages.failedToUpdateStatusMessage
import ru.kode.android.build.publish.plugin.jira.messages.notPossibleToUpdateStatusMessage
import ru.kode.android.build.publish.plugin.jira.service.network.JiraService

/**
 * Parameters for the [SetStatusWork] work action.
 *
 * This interface defines the input parameters required to update Jira issue statuses.
 * It's used to pass data to the [SetStatusWork] work action.
 */
internal interface SetStatusParameters : WorkParameters {
    /**
     * The set of Jira issue keys to update (e.g., ["PROJ-123", "PROJ-456"])
     */
    val issues: SetProperty<String>

    /**
     * The ID of the status transition to apply to the issues
     */
    val statusTransitionId: Property<String>

    /**
     * The network service used to communicate with the Jira API
     */
    val service: Property<JiraService>

    /**
     * The logger service used to log messages from the work action.
     */
    val loggerService: Property<LoggerService>
}

/**
 * A Gradle work action that updates the status of multiple Jira issues.
 *
 * This work action is responsible for:
 * 1. Taking a set of Jira issue keys and a target status transition ID
 * 2. Attempting to update each issue's status using the provided [JiraService]
 * 3. Gracefully handling and logging any failures without failing the entire build
 *
 * Failures to update individual issues are logged but don't prevent other issues from being processed.
 *
 * ## Error Handling
 * - Network errors and API failures are caught and logged at INFO level
 * - The work continues processing remaining issues even if some fail
 *
 * @see [JiraService.setStatus] for the actual API call implementation
 */
internal abstract class SetStatusWork : WorkAction<SetStatusParameters> {
    override fun execute() {
        val issues = parameters.issues.get().toList()
        val service = parameters.service.get()
        val logger = parameters.loggerService.get()
        val statusTransitionId = parameters.statusTransitionId.orNull

        if (statusTransitionId != null) {
            issues.forEach { issue ->
                try {
                    service.setStatus(issue, statusTransitionId)
                } catch (ex: UploadError) {
                    logger.info(failedToUpdateStatusMessage(issue), ex)
                }
            }
        } else {
            logger.info(notPossibleToUpdateStatusMessage(issues))
        }
    }
}
