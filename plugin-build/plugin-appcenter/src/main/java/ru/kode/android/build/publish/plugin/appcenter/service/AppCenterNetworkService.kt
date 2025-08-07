package ru.kode.android.build.publish.plugin.appcenter.service

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

abstract class AppCenterNetworkService
    @Inject
    constructor() : BuildService<AppCenterNetworkService.Params> {
        interface Params : BuildServiceParameters {
            val token: RegularFileProperty
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

        internal fun initUploadApi(uploadDomain: String) {
            uploadApiProperty.set(
                retrofitBuilderProperty.map {
                    it.baseUrl(uploadDomain).build().create(AppCenterUploadApi::class.java)
                },
            )
        }

        internal fun initAppName(appName: String) {
            appNameProperty.set(appName)
        }

        internal fun prepareRelease(
            buildVersion: String,
            buildNumber: String,
        ): PrepareResponse {
            val request = PrepareReleaseRequest(buildVersion, buildNumber)
            return api.prepareRelease(ownerName, appName, request).executeOrThrow()
        }

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

        internal fun sendUploadIsFinished(
            packageAssetId: String,
            encodedToken: String,
        ) {
            uploadApi
                .sendUploadIsFinished(packageAssetId, encodedToken)
                .executeOrThrow()
        }

        internal fun commit(preparedUploadId: String) {
            api.commit(ownerName, appName, preparedUploadId, CommitRequest(preparedUploadId))
                .executeOrThrow()
        }

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
