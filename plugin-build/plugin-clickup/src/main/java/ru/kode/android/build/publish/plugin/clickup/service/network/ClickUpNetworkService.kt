package ru.kode.android.build.publish.plugin.clickup.service.network

import com.squareup.moshi.Moshi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import ru.kode.android.build.publish.plugin.clickup.task.automation.api.ClickUpApi
import ru.kode.android.build.publish.plugin.clickup.task.automation.entity.AddFieldToTaskRequest
import ru.kode.android.build.publish.plugin.core.util.addProxyIfAvailable
import ru.kode.android.build.publish.plugin.core.util.executeOptionalOrThrow
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val API_BASE_URL = "https://api.clickup.com/api/"
private const val HTTP_CONNECT_TIMEOUT_SECONDS = 60L

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
abstract class ClickUpNetworkService
    @Inject
    constructor() : BuildService<ClickUpNetworkService.Params> {
        /**
         * Configuration parameters for the [ClickUpNetworkService].
         */
        interface Params : BuildServiceParameters {
            /**
             *  A file containing the ClickUp API token for authentication.
             *  The file should contain the token as plain text with no additional formatting.
             */
            val token: RegularFileProperty
        }

        internal abstract val okHttpClientProperty: Property<OkHttpClient>

        internal abstract val apiProperty: Property<ClickUpApi>

        init {
            okHttpClientProperty.set(
                parameters.token.map { token ->
                    OkHttpClient.Builder()
                        .connectTimeout(HTTP_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .readTimeout(HTTP_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .writeTimeout(HTTP_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .addInterceptor(AttachTokenInterceptor(token.asFile.readText()))
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
                okHttpClientProperty.map { client ->
                    val moshi = Moshi.Builder().build()
                    Retrofit.Builder()
                        .baseUrl(API_BASE_URL)
                        .client(client)
                        .addConverterFactory(MoshiConverterFactory.create(moshi))
                        .build()
                        .create(ClickUpApi::class.java)
                },
            )
        }

        private val api: ClickUpApi get() = apiProperty.get()

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
            api.addTagToTask(taskId, tagName).executeOptionalOrThrow()
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
            val request = AddFieldToTaskRequest(value = fieldValue)
            api.addFieldToTask(taskId, fieldId, request).executeOptionalOrThrow()
        }

        companion object {
            private val logger: Logger = Logging.getLogger(ClickUpNetworkService::class.java)
        }
    }

private class AttachTokenInterceptor(
    private val apiToken: String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val originalRequest = chain.request()
        val newRequest =
            originalRequest.newBuilder()
                .addHeader(name = "Content-Type", "application/json")
                .addHeader(name = "Authorization", apiToken)
                .build()
        return chain.proceed(newRequest)
    }
}
