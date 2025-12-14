package ru.kode.android.build.publish.plugin.clickup.service.network

import okhttp3.OkHttpClient
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import ru.kode.android.build.publish.plugin.clickup.controller.ClickUpController
import ru.kode.android.build.publish.plugin.clickup.controller.ClickUpControllerImpl
import ru.kode.android.build.publish.plugin.clickup.network.api.ClickUpApi
import ru.kode.android.build.publish.plugin.clickup.network.factory.ClickUpApiFactory
import ru.kode.android.build.publish.plugin.clickup.network.factory.ClickUpClientFactory
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
 * @see ClickUpApi For the actual API endpoint definitions
 */
abstract class ClickUpService
    @Inject
    constructor() : BuildService<ClickUpService.Params> {
        /**
         * Configuration parameters for the [ClickUpService].
         */
        interface Params : BuildServiceParameters {
            /**
             *  A file containing the ClickUp API token for authentication.
             *  The file should contain the token as plain text with no additional formatting.
             */
            val token: RegularFileProperty
        }

        private val logger: Logger = Logging.getLogger("ClickUp")


        internal abstract val okHttpClientProperty: Property<OkHttpClient>

        internal abstract val apiProperty: Property<ClickUpApi>

        internal abstract val controllerProperty: Property<ClickUpController>

        init {
            okHttpClientProperty.set(
                parameters.token.map { token ->
                    val token = token.asFile.readText()
                    ClickUpClientFactory.build(token, logger)
                },
            )
            apiProperty.set(
                okHttpClientProperty.map { client ->
                    ClickUpApiFactory.build(client)
                },
            )
            controllerProperty.set(
                apiProperty.map { api ->
                    ClickUpControllerImpl(api, logger)
                },
            )
        }

        private val controller: ClickUpController get() = controllerProperty.get()


        /**
         * Retrieves the ID of a ClickUp custom field.
         *
         * @param workspaceName The name of the workspace where the custom field is located.
         * @param fieldName The name of the custom field.
         * @return The ID of the custom field.
         */
        fun getCustomFieldId(workspaceName: String, fieldName: String): String {
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
    }
