package ru.kode.android.build.publish.plugin.clickup.service.network

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import ru.kode.android.build.publish.plugin.clickup.controller.ClickUpController
import ru.kode.android.build.publish.plugin.clickup.controller.addFixVersionToTasks
import ru.kode.android.build.publish.plugin.clickup.controller.addTagToTasks
import ru.kode.android.build.publish.plugin.clickup.controller.factory.ClickUpControllerFactory
import ru.kode.android.build.publish.plugin.core.logger.LoggerService
import javax.inject.Inject

/**
 * A Gradle build service that provides network operations for interacting with the ClickUp API.
 *
 * This service is responsible for:
 * - Managing HTTP client configuration and lifecycle
 * - Handling authentication with the ClickUp API
 * - Executing API requests with proper error handling
 * - Managing connection timeouts and retries
 *
 * The service is implemented as a [BuildService] to ensure proper resource cleanup
 * and to maintain a single HTTP client instance across multiple tasks.
 *
 * @see BuildService For more information about Gradle build services
 * @see ClickUpController For the actual controller interface
 */
abstract class ClickUpService
    @Inject
    constructor() : BuildService<ClickUpService.Params> {
        /**
         * Configuration parameters for the [ClickUpService].
         */
        interface Params : BuildServiceParameters {
            /**
             * A file containing the ClickUp API token for authentication.
             * The file should contain the token as plain text with no additional formatting.
             */
            val token: RegularFileProperty

            /**
             * A Gradle service that provides logging capabilities for the [ClickUpService].
             *
             * @see LoggerService
             */
            val loggerService: Property<LoggerService>
        }

        private val controller: ClickUpController by lazy {
            ClickUpControllerFactory.build(
                token = parameters.token.get().asFile.readText(),
                logger = parameters.loggerService.get().logger,
            )
        }

        /**
         * Retrieves the ID of a ClickUp custom field.
         *
         * @param workspaceName The name of the workspace where the custom field is located.
         * @param fieldName The name of the custom field.
         * @return The ID of the custom field.
         */
        fun getCustomFieldId(
            workspaceName: String,
            fieldName: String,
        ): String {
            return controller.getOrCreateCustomFieldId(workspaceName, fieldName)
        }

        /**
         * Adds a tag to a ClickUp task.
         *
         * @param taskId The ID of the ClickUp task to tag
         * @param tagName The name of the tag to add
         *
         * @throws IOException If the network request fails
         * @throws RuntimeException If the API returns an error response
         */
        fun addTagToTask(
            taskId: String,
            tagName: String,
        ) {
            controller.addTagToTask(taskId, tagName)
        }

        /**
         * Adds or updates a custom field value for a ClickUp task.
         *
         * @param taskId The ID of the ClickUp task to update
         * @param fieldId The ID of the custom field to set
         * @param fieldValue The value to set for the custom field
         *
         * @throws IOException If the network request fails
         * @throws RuntimeException If the API returns an error response or the field ID is invalid
         */
        fun addFieldToTask(
            taskId: String,
            fieldId: String,
            fieldValue: String,
        ) {
            controller.addFieldToTask(taskId, fieldId, fieldValue)
        }

        /**
         * Adds a tag to multiple ClickUp tasks.
         *
         * @param tagName The name of the tag to add
         * @param taskIds The IDs of the ClickUp tasks to tag
         * @param log Callback invoked with human-readable progress messages
         *
         * @throws IOException If the network request fails
         * @throws RuntimeException If the API returns an error response
         */
        fun addTagToTasks(
            tagName: String,
            taskIds: Collection<String>,
            log: (String) -> Unit,
        ) = controller.addTagToTasks(tagName, taskIds, log)

        /**
         * Adds or updates the fix version custom field for multiple ClickUp tasks.
         *
         * @param workspaceName The name of the workspace where the custom field is located
         * @param fieldName The name of the custom field to set
         * @param version The fix version value to set
         * @param taskIds The IDs of the ClickUp tasks to update
         * @param log Callback invoked with human-readable progress messages
         *
         * @throws IOException If the network request fails
         * @throws RuntimeException If the API returns an error response or the field ID is invalid
         */
        fun addFixVersionToTasks(
            workspaceName: String,
            fieldName: String,
            version: String,
            taskIds: Collection<String>,
            log: (String) -> Unit,
        ) = controller.addFixVersionToTasks(workspaceName, fieldName, version, taskIds, log)
    }
