package ru.kode.android.build.publish.plugin.jira.service.network

import okhttp3.OkHttpClient
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import ru.kode.android.build.publish.plugin.core.api.config.BasicAuthCredentials
import ru.kode.android.build.publish.plugin.core.logger.LoggerService
import ru.kode.android.build.publish.plugin.jira.controller.JiraController
import ru.kode.android.build.publish.plugin.jira.controller.JiraControllerImpl
import ru.kode.android.build.publish.plugin.jira.network.api.JiraApi
import ru.kode.android.build.publish.plugin.jira.network.factory.JiraApiFactory
import ru.kode.android.build.publish.plugin.jira.network.factory.JiraClientFactory
import javax.inject.Inject

/**
 * A Gradle build service that provides network operations for interacting with Jira's REST API.
 *
 * This service handles authentication, request/response serialization, and error handling
 * for Jira API operations. It's designed to be used within Gradle build scripts to
 * automate Jira-related tasks as part of the build process.

 * @see BuildService
 * @see JiraApi
 */
abstract class JiraService
    @Inject
    constructor() : BuildService<JiraService.Params> {
        /**
         * Configuration parameters for the Jira network service.
         *
         * @see BasicAuthCredentials
         */
        interface Params : BuildServiceParameters {
            /**
             * The base URL of the Jira instance (e.g., "https://your-domain.atlassian.net")
             */
            val baseUrl: Property<String>

            /**
             * The authentication credentials for the Jira API, containing username and password/token
             */
            val credentials: Property<BasicAuthCredentials>

            /**
             * The logger service for logging messages during network operations.
             */
            val loggerService: Property<LoggerService>
        }

        internal abstract val okHttpClientProperty: Property<OkHttpClient>

        internal abstract val apiProperty: Property<JiraApi>

        internal abstract val controllerProperty: Property<JiraController>

        init {
            val username = parameters.credentials.flatMap { it.username }
            val password = parameters.credentials.flatMap { it.password }
            okHttpClientProperty.set(
                parameters.loggerService.flatMap { logger ->
                    username
                        .zip(password) { username, password ->
                            JiraClientFactory.build(username, password, logger)
                        }
                },
            )
            apiProperty.set(
                okHttpClientProperty
                    .zip(parameters.baseUrl) { client, baseUrl ->
                        JiraApiFactory.build(client, baseUrl)
                    },
            )
            controllerProperty.set(
                apiProperty.zip(parameters.loggerService) { api, logger ->
                    JiraControllerImpl(api, logger)
                },
            )
        }

        private val controller: JiraController get() = controllerProperty.get()

        /**
         * Retrieves the ID of the status with the given name in the specified project,
         * using the first issue in the list that has a transition to this status.
         *
         * @param projectKey The key of the Jira project
         * @param statusName The name of the status to search for
         * @param issues A list of issue keys to search for a transition to the given status
         * @return The ID of the status with the given name
         *
         * @throws IOException If the network request fails
         * @throws JiraApiException If the Jira API returns an error
         */
        fun getStatusTransitionId(
            projectKey: String,
            statusName: String,
            issues: List<String>,
        ): String {
            return controller.getStatusTransitionId(projectKey, statusName, issues)
        }

        /**
         * Retrieves the ID of a Jira project by its key.
         *
         * @param projectKey The key of the project (e.g., "PROJECT")
         *
         * @return The ID of the project
         *
         * @throws IOException If the network request fails
         * @throws JiraApiException If the Jira API returns an error
         */
        fun getProjectId(projectKey: String): Long {
            return controller.getProjectId(projectKey)
        }

        /**
         * Transitions a Jira issue to a new status.
         *
         * @param issueKey The issue key (e.g., "PROJ-123")
         * @param statusTransitionId The ID of the status transition to execute
         *
         * @throws IOException If the network request fails
         * @throws JiraApiException If the Jira API returns an error
         */
        fun setStatus(
            issueKey: String,
            statusTransitionId: String,
        ) {
            controller.setIssueStatus(issueKey, statusTransitionId)
        }

        /**
         * Adds a label to a Jira issue.
         *
         * @param issueKey The issue key (e.g., "PROJ-123")
         * @param label The label to add
         *
         * @throws IOException If the network request fails
         * @throws JiraApiException If the Jira API returns an error
         */
        fun addLabel(
            issueKey: String,
            label: String,
        ) {
            controller.addIssueLabel(issueKey, label)
        }

        /**
         * Creates a new version in a Jira project.
         *
         * @param projectId The ID of the Jira project
         * @param version The version name to create
         *
         * @throws IOException If the network request fails
         * @throws JiraApiException If the Jira API returns an error or version already exists
         */
        fun createVersion(
            projectId: Long,
            version: String,
        ) {
            controller.createProjectVersion(projectId, version)
        }

        /**
         * Adds a fix version to a Jira issue.
         *
         * @param issueKey The issue key (e.g., "PROJ-123")
         * @param version The version to add as a fix version
         *
         * @throws IOException If the network request fails
         * @throws JiraApiException If the Jira API returns an error
         */
        fun addFixVersion(
            issueKey: String,
            version: String,
        ) {
            controller.addIssueFixVersion(issueKey, version)
        }
    }
