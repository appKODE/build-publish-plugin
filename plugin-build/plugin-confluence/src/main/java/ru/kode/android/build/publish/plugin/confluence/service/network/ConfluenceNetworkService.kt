package ru.kode.android.build.publish.plugin.confluence.service.network

import com.squareup.moshi.Moshi
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import ru.kode.android.build.publish.plugin.confluence.task.distribution.api.ConfluenceApi
import ru.kode.android.build.publish.plugin.confluence.task.distribution.entity.AddCommentRequest
import ru.kode.android.build.publish.plugin.confluence.task.distribution.entity.Body
import ru.kode.android.build.publish.plugin.confluence.task.distribution.entity.Container
import ru.kode.android.build.publish.plugin.confluence.task.distribution.entity.Storage
import ru.kode.android.build.publish.plugin.core.api.config.BasicAuthCredentials
import ru.kode.android.build.publish.plugin.core.util.addProxyIfAvailable
import ru.kode.android.build.publish.plugin.core.util.executeOrThrow
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val HTTP_CONNECT_TIMEOUT_MINUTES = 3L

private val logger: Logger = Logging.getLogger(ConfluenceNetworkService::class.java)

/**
 * A network service for interacting with the Confluence REST API.
 *
 * This service provides functionality to:
 * - Upload files as attachments to Confluence pages
 * - Add comments with download links to Confluence pages
 * - Handle authentication with Confluence instances
 *
 * It uses Retrofit for type-safe HTTP client operations and OkHttp for the underlying network stack.
 * The service is designed to be used as a Gradle BuildService for better resource management.
 */
abstract class ConfluenceNetworkService
    @Inject
    constructor() : BuildService<ConfluenceNetworkService.Params> {
        /**
         * Configuration parameters for the ConfluenceNetworkService.
         *
         * This interface defines the required configuration for initializing the service.
         * The parameters are provided through Gradle's configuration avoidance API.
         */
        interface Params : BuildServiceParameters {
            /**
             *  The base URL of the Confluence instance (e.g., "https://your-domain.atlassian.net/wiki/")
             */
            val baseUrl: Property<String>

            /**
             * The authentication credentials for the Confluence API, typically username and password
             */
            val credentials: Property<BasicAuthCredentials>
        }

        internal abstract val okHttpClientProperty: Property<OkHttpClient>
        internal abstract val apiProperty: Property<ConfluenceApi>

        init {
            okHttpClientProperty.set(
                parameters.credentials.flatMap { it.username }
                    .zip(parameters.credentials.flatMap { it.password }) { username, password ->
                        OkHttpClient.Builder()
                            .connectTimeout(HTTP_CONNECT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
                            .readTimeout(HTTP_CONNECT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
                            .writeTimeout(HTTP_CONNECT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
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
                        .create(ConfluenceApi::class.java)
                },
            )
        }

        private val api: ConfluenceApi get() = apiProperty.get()
        private val baseUrl: String = parameters.baseUrl.get()

        /**
         * Uploads a file as an attachment to a Confluence page.
         *
         * This method handles the multipart form data upload process to the Confluence REST API.
         * It will throw an exception if the upload fails for any reason.
         *
         * @param pageId The ID of the Confluence page where the file should be attached
         * @param file The file to upload. Must be a valid, readable file.
         *
         * @throws IllegalStateException if the file doesn't exist or is not readable
         * @throws Exception if the API request fails or returns an error
         */
        fun uploadFile(
            pageId: String,
            file: File,
        ) {
            val filePart =
                MultipartBody.Part.createFormData(
                    "file",
                    file.name,
                    file.asRequestBody(),
                )
            api.uploadAttachment(
                pageId = pageId,
                file = filePart,
            ).executeOrThrow()
        }

        /**
         * Adds a comment with a file download link to a Confluence page.
         *
         * The comment will include a direct download link to the attached file.
         * The file must have been previously uploaded to the page.
         *
         * @param pageId The ID of the Confluence page where the comment should be added
         * @param fileName The name of the file to create a download link for
         *
         * @throws IllegalArgumentException if the pageId is empty or invalid
         * @throws Exception if the API request fails or returns an error
         */
        fun addComment(
            pageId: String,
            fileName: String,
        ) {
            val comment = "<a href=\"$baseUrl/download/attachments/$pageId/$fileName\">$fileName</a>"
            api.addComment(
                commentRequest =
                    AddCommentRequest(
                        container = Container(pageId),
                        body = Body(Storage(comment)),
                    ),
            ).executeOrThrow()
        }
    }

private class AttachTokenInterceptor(
    private val username: String,
    private val password: String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val newRequest =
            originalRequest.newBuilder()
                .addHeader(name = "Authorization", Credentials.basic(username, password))
                .build()
        return chain.proceed(newRequest)
    }
}
