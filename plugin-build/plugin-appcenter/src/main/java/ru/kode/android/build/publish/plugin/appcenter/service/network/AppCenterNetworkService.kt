package ru.kode.android.build.publish.plugin.appcenter.service.network

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
import ru.kode.android.build.publish.plugin.appcenter.task.distribution.api.AppCenterApi
import ru.kode.android.build.publish.plugin.appcenter.task.distribution.api.AppCenterUploadApi
import ru.kode.android.build.publish.plugin.appcenter.task.distribution.entity.ChunkRequestBody
import ru.kode.android.build.publish.plugin.appcenter.task.distribution.entity.CommitRequest
import ru.kode.android.build.publish.plugin.appcenter.task.distribution.entity.DistributeRequest
import ru.kode.android.build.publish.plugin.appcenter.task.distribution.entity.GetUploadResponse
import ru.kode.android.build.publish.plugin.appcenter.task.distribution.entity.PrepareReleaseRequest
import ru.kode.android.build.publish.plugin.appcenter.task.distribution.entity.PrepareResponse
import ru.kode.android.build.publish.plugin.appcenter.task.distribution.entity.SendMetaDataResponse
import ru.kode.android.build.publish.plugin.core.util.UploadException
import ru.kode.android.build.publish.plugin.core.util.addProxyIfAvailable
import ru.kode.android.build.publish.plugin.core.util.executeOrThrow
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val API_BASE_URL = "https://api.appcenter.ms/v0.1/"
private const val HTTP_CONNECT_TIMEOUT_SEC = 60L

/**
 * Build service that manages communication with AppCenter APIs for release upload and distribution.
 *
 * This service handles authentication, prepares releases, uploads APK files in chunks,
 * commits releases, and distributes them to tester groups on AppCenter.
 *
 * The service uses Retrofit with Moshi for JSON serialization and OkHttp for HTTP communication,
 * with token-based authentication injected via an interceptor.
 *
 * @constructor Creates an instance of the service with injected parameters.
 */
abstract class AppCenterNetworkService
    @Inject
    constructor() : BuildService<AppCenterNetworkService.Params> {
        /**
         * Parameters required for configuring the service.
         */
        interface Params : BuildServiceParameters {
            /**
             * File containing the authentication token for AppCenter API.
             */
            val token: RegularFileProperty

            /**
             * The owner (organization or user) name for the AppCenter app.
             */
            val ownerName: Property<String>
        }

        internal abstract val okHttpClientProperty: Property<OkHttpClient>
        internal abstract val apiProperty: Property<AppCenterApi>
        internal abstract val retrofitBuilderProperty: Property<Retrofit.Builder>
        internal abstract val uploadApiProperty: Property<AppCenterUploadApi>
        internal abstract val appNameProperty: Property<String>

        init {
            okHttpClientProperty.set(
                parameters.token.map { token ->
                    OkHttpClient.Builder()
                        .connectTimeout(HTTP_CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
                        .readTimeout(HTTP_CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
                        .writeTimeout(HTTP_CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
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
            retrofitBuilderProperty.set(
                okHttpClientProperty.map { client ->
                    val moshi = Moshi.Builder().build()
                    Retrofit.Builder()
                        .client(client)
                        .addConverterFactory(MoshiConverterFactory.create(moshi))
                },
            )
            apiProperty.set(
                retrofitBuilderProperty.map {
                    it.baseUrl(API_BASE_URL).build().create(AppCenterApi::class.java)
                },
            )
        }

        private val api: AppCenterApi
            get() = apiProperty.get()

        private val uploadApi: AppCenterUploadApi
            get() = uploadApiProperty.get()

        private val ownerName: String
            get() = parameters.ownerName.get()

        private val appName: String
            get() = appNameProperty.get()

        /**
         * Initializes the upload API client with a specific upload domain.
         *
         * @param uploadDomain Base URL domain for upload-related API calls.
         */
        internal fun initUploadApi(uploadDomain: String) {
            uploadApiProperty.set(
                retrofitBuilderProperty.map {
                    it.baseUrl(uploadDomain).build().create(AppCenterUploadApi::class.java)
                },
            )
        }

        /**
         * Sets the app name for subsequent API calls.
         *
         * @param appName Application name registered in AppCenter.
         */
        internal fun initAppName(appName: String) {
            appNameProperty.set(appName)
        }

        /**
         * Prepares a new release on AppCenter with version and build number.
         *
         * @param buildVersion Version string for the build (e.g., "1.0.0").
         * @param buildNumber Numeric or string build identifier.
         * @return The response containing upload URLs and IDs.
         * @throws IOException on network failure or API error.
         */
        internal fun prepareRelease(
            buildVersion: String,
            buildNumber: String,
        ): PrepareResponse {
            val request = PrepareReleaseRequest(buildVersion, buildNumber)
            return api.prepareRelease(ownerName, appName, request).executeOrThrow()
        }

        /**
         * Sends metadata for the APK file to AppCenter upload service.
         *
         * @param apkFile The APK file to upload metadata for.
         * @param packageAssetId Package asset identifier received from prepareRelease.
         * @param encodedToken Encoded upload token for authorization.
         * @return Metadata response including chunk info for file upload.
         * @throws UploadException if the response status is not success.
         */
        internal fun sendMetaData(
            apkFile: File,
            packageAssetId: String,
            encodedToken: String,
        ): SendMetaDataResponse {
            val contentType = "application/vnd.android.package-archive"
            val response =
                uploadApi.sendMetaData(
                    packageAssetId = packageAssetId,
                    fileName = apkFile.name,
                    fileSize = apkFile.length(),
                    encodedToken = encodedToken,
                    contentType = contentType,
                ).executeOrThrow()
            if (response.status_code != "Success") {
                throw UploadException("send meta data terminated with ${response.status_code}")
            }
            return response
        }

        /**
         * Uploads a chunk of the APK file to AppCenter.
         *
         * @param packageAssetId Package asset identifier for the upload.
         * @param encodedToken Encoded upload token for authorization.
         * @param chunkNumber Index of the chunk to upload.
         * @param request The chunk request body containing the file data.
         * @throws IOException on network or API failure.
         */
        internal fun uploadChunk(
            packageAssetId: String,
            encodedToken: String,
            chunkNumber: Int,
            request: ChunkRequestBody,
        ) {
            return uploadApi
                .uploadChunk(packageAssetId, encodedToken, chunkNumber, request)
                .executeOrThrow()
        }

        /**
         * Signals to AppCenter that all chunks have been uploaded.
         *
         * @param packageAssetId Package asset identifier.
         * @param encodedToken Encoded upload token.
         */
        internal fun sendUploadIsFinished(
            packageAssetId: String,
            encodedToken: String,
        ) {
            uploadApi
                .sendUploadIsFinished(packageAssetId, encodedToken)
                .executeOrThrow()
        }

        /**
         * Commits the uploaded release on AppCenter.
         *
         * @param preparedUploadId Identifier of the prepared upload.
         */
        internal fun commit(preparedUploadId: String) {
            api.commit(ownerName, appName, preparedUploadId, CommitRequest(preparedUploadId))
                .executeOrThrow()
        }

        /**
         * Polls AppCenter until the release is ready to be published or the max request count is exceeded.
         *
         * @param preparedUploadId Upload identifier to check status for.
         * @param maxRequestCount Maximum number of polling attempts before failure.
         * @param requestDelayMs Delay in milliseconds between polling attempts.
         * @return The upload status response containing release distinct id when ready.
         * @throws IllegalStateException if max request count is exceeded before ready.
         */
        internal fun waitingReadyToBePublished(
            preparedUploadId: String,
            maxRequestCount: Int,
            requestDelayMs: Long,
        ): GetUploadResponse {
            var requestCount = 0
            var response: GetUploadResponse
            do {
                response = getUpload(preparedUploadId)
                Thread.sleep(requestDelayMs)
                requestCount++
                require(requestCount <= maxRequestCount) { "Cannot fetch upload status" }
            } while (response.release_distinct_id == null)
            return response
        }

        private fun getUpload(preparedUploadId: String): GetUploadResponse {
            return api.getUpload(ownerName, appName, preparedUploadId).executeOrThrow()
        }

        /**
         * Distributes the uploaded release to specified tester groups with release notes.
         *
         * @param releaseId Release distinct identifier.
         * @param distributionGroups Set of tester group names.
         * @param releaseNotes Text notes to accompany the release.
         */
        internal fun distribute(
            releaseId: String,
            distributionGroups: Set<String>,
            releaseNotes: String,
        ) {
            val request =
                DistributeRequest(
                    destinations = distributionGroups.map { DistributeRequest.Destination(it) },
                    release_notes = releaseNotes,
                )
            api.distribute(ownerName, appName, releaseId, request).executeOrThrow()
        }

        companion object {
            private val logger: Logger = Logging.getLogger(AppCenterNetworkService::class.java)
        }
    }

private class AttachTokenInterceptor(
    private val token: String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val originalRequest = chain.request()
        val newRequest =
            originalRequest.newBuilder()
                .addHeader(name = "Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .addHeader(name = "X-API-Token", token)
                .build()
        return chain.proceed(newRequest)
    }
}
