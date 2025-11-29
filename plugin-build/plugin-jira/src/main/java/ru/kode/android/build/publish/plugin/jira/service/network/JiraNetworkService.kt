package ru.kode.android.build.publish.plugin.jira.service.network

import com.squareup.moshi.Moshi
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import ru.kode.android.build.publish.plugin.core.api.config.BasicAuthCredentials
import ru.kode.android.build.publish.plugin.core.util.addProxyIfAvailable
import ru.kode.android.build.publish.plugin.core.util.executeOptionalOrThrow
import ru.kode.android.build.publish.plugin.jira.task.automation.api.JiraApi
import ru.kode.android.build.publish.plugin.jira.task.automation.entity.AddFixVersionRequest
import ru.kode.android.build.publish.plugin.jira.task.automation.entity.AddLabelRequest
import ru.kode.android.build.publish.plugin.jira.task.automation.entity.CreateVersionRequest
import ru.kode.android.build.publish.plugin.jira.task.automation.entity.SetStatusRequest
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val HTTP_CONNECT_TIMEOUT_SECONDS = 30L

private val logger: Logger = Logging.getLogger(JiraNetworkService::class.java)

/**
 * A Gradle build service that provides network operations for interacting with Jira's REST API.
 *
 * This service handles authentication, request/response serialization, and error handling
 * for Jira API operations. It's designed to be used within Gradle build scripts to
 * automate Jira-related tasks as part of the build process.

 * @see BuildService
 * @see JiraApi
 */
abstract class JiraNetworkService
    @Inject
    constructor() : BuildService<JiraNetworkService.Params> {
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

        init {
            val username = parameters.credentials.flatMap { it.username }
            val password = parameters.credentials.flatMap { it.password }
            okHttpClientProperty.set(
                username
                    .zip(password) { username, password ->
                        OkHttpClient.Builder()
                            .connectTimeout(HTTP_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                            .readTimeout(HTTP_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                            .writeTimeout(HTTP_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                            .addInterceptor(AttachTokenInterceptor(username, password))
                            .addProxyIfAvailable()
                            .apply {
                                val loggingInterceptor = HttpLoggingInterceptor { message -> logger.info(message) }
                                loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
                                addNetworkInterceptor(loggingInterceptor)
                            }
                            .build()
                    },
            )
            apiProperty.set(
                okHttpClientProperty.zip(parameters.baseUrl) { client, baseUrl ->
                    val moshi = Moshi.Builder().build()
                    Retrofit.Builder()
                        .baseUrl(baseUrl)
                        .client(client)
                        .addConverterFactory(MoshiConverterFactory.create(moshi))
                        .build()
                        .create(JiraApi::class.java)
                },
            )
        }

        private val api: JiraApi get() = apiProperty.get()

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
            val request =
                SetStatusRequest(
                    transition =
                        SetStatusRequest.Transition(
                            id = statusTransitionId,
                        ),
                )
            api.setStatus(issue, request).executeOptionalOrThrow()
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
            val request =
                AddLabelRequest(
                    update =
                        AddLabelRequest.Update(
                            labels = listOf(AddLabelRequest.Label(label)),
                        ),
                )
            api.addLabel(issue, request).executeOptionalOrThrow()
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
            val request =
                CreateVersionRequest(
                    name = version,
                    projectId = projectId,
                )
            api.createVersion(request).executeOptionalOrThrow()
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
            val request =
                AddFixVersionRequest(
                    update =
                        AddFixVersionRequest.Update(
                            fixVersions =
                                listOf(
                                    AddFixVersionRequest.FixVersion(
                                        AddFixVersionRequest.FixVersion.Description(
                                            name = version,
                                        ),
                                    ),
                                ),
                        ),
                )
            api.addFixVersion(issue, request).executeOptionalOrThrow()
        }
    }

private class AttachTokenInterceptor(
    private val username: String,
    private val password: String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain) =
        chain.proceed(
            chain.request()
                .newBuilder()
                .addHeader(name = "Content-Type", "application/json")
                .addHeader("Authorization", Credentials.basic(username, password))
                .build(),
        )
}
