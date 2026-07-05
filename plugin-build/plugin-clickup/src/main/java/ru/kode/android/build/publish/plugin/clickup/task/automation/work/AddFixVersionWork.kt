package ru.kode.android.build.publish.plugin.clickup.task.automation.work

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.clickup.service.network.ClickUpService
import ru.kode.android.build.publish.plugin.core.logger.LoggerService

/**
 * Parameters for the [AddFixVersionWork] work action.
 *
 * This interface defines the input parameters required to update the fix version
 * for ClickUp tasks. It's used by Gradle's worker API to pass data to the work action.
 */
internal interface AddFixVersionParameters : WorkParameters {
    /**
     * The name of the ClickUp account the tasks live on.
     */
    val accountName: Property<String>

    /**
     * The ClickUp workspace (team) name the tasks live in.
     */
    val workspaceName: Property<String>

    /**
     * The name of the custom field that stores the fix version.
     */
    val fieldName: Property<String>

    /**
     * The version string to set for the tasks.
     */
    val version: Property<String>

    /**
     * The set of ClickUp task IDs to update.
     */
    val issues: SetProperty<String>

    /**
     * The network service used to communicate with the ClickUp API.
     */
    val service: Property<ClickUpService>

    /**
     * The logger service used to report progress.
     */
    val loggerService: Property<LoggerService>
}

/**
 * A Gradle work action that updates the fix version for multiple ClickUp tasks.
 *
 * This work action is executed asynchronously by Gradle's worker API. It resolves the custom fix-version
 * field for the account's workspace and updates it with the provided version string for each task.
 *
 * @see WorkAction For more information about Gradle work actions
 * @see ClickUpService For the underlying network operations
 */
internal abstract class AddFixVersionWork : WorkAction<AddFixVersionParameters> {
    override fun execute() {
        parameters.service.get().addFixVersionToTasks(
            accountName = parameters.accountName.get(),
            workspaceName = parameters.workspaceName.get(),
            fieldName = parameters.fieldName.get(),
            version = parameters.version.get(),
            taskIds = parameters.issues.get(),
            log = parameters.loggerService.get()::info,
        )
    }
}
