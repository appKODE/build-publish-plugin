package ru.kode.android.build.publish.plugin.jira.service.network

import okhttp3.OkHttpClient
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import ru.kode.android.build.publish.plugin.core.api.config.BasicAuthCredentials
import ru.kode.android.build.publish.plugin.jira.controller.JiraControllerImpl
import ru.kode.android.build.publish.plugin.jira.network.factory.JiraApiFactory
import ru.kode.android.build.publish.plugin.jira.network.factory.JiraClientFactory
import ru.kode.android.build.publish.plugin.jira.network.api.JiraApi
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
        }

        internal abstract val okHttpClientProperty: Property<OkHttpClient>

        internal abstract val apiProperty: Property<JiraApi>

        internal abstract val controllerProperty: Property<JiraControllerImpl>

        init {
            val username = parameters.credentials.flatMap { it.username }
            val password = parameters.credentials.flatMap { it.password }
            okHttpClientProperty.set(
                username
                    .zip(password) { username, password ->
                        JiraClientFactory.build(username, password)
                    },
            )
            apiProperty.set(
                okHttpClientProperty
                    .zip(parameters.baseUrl) { client, baseUrl ->
                        JiraApiFactory.build(client, baseUrl)
                    },
            )
            controllerProperty.set(
                apiProperty.map { api -> JiraControllerImpl(api) }
            )
        }

        private val controller: JiraControllerImpl get() = controllerProperty.get()

        /**
         * Transitions a Jira issue to a new status.
         *
         * @param issue The issue key (e.g., "PROJ-123")
         * @param statusTransitionId The ID of the status transition to execute
         *
         * @throws IOException If the network request fails
         * @throws JiraApiException If the Jira API returns an error
         */
        fun setStatus(
            issue: String,
            statusTransitionId: String,
        ) {
            controller.setIssueStatus(issue, statusTransitionId)
        }

        /**
         * Adds a label to a Jira issue.
         *
         * @param issue The issue key (e.g., "PROJ-123")
         * @param label The label to add
         *
         * @throws IOException If the network request fails
         * @throws JiraApiException If the Jira API returns an error
         */
        fun addLabel(
            issue: String,
            label: String,
        ) {
            controller.addIssueLabel(issue, label)
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
         * @param issue The issue key (e.g., "PROJ-123")
         * @param version The version to add as a fix version
         *
         * @throws IOException If the network request fails
         * @throws JiraApiException If the Jira API returns an error
         */
        fun addFixVersion(
            issue: String,
            version: String,
        ) {
            controller.addIssueFixVersion(issue, version)
        }
    }
