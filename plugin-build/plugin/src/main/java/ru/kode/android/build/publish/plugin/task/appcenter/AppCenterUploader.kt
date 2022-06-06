package ru.kode.android.build.publish.plugin.task.appcenter

import com.squareup.moshi.Moshi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.gradle.api.logging.Logger
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import ru.kode.android.build.publish.plugin.task.appcenter.api.AppCenterApi
import ru.kode.android.build.publish.plugin.task.appcenter.api.AppCenterUploadApi
import ru.kode.android.build.publish.plugin.task.appcenter.entity.ChunkRequestBody
import ru.kode.android.build.publish.plugin.task.appcenter.entity.CommitRequest
import ru.kode.android.build.publish.plugin.task.appcenter.entity.DistributeRequest
import ru.kode.android.build.publish.plugin.task.appcenter.entity.GetUploadResponse
import ru.kode.android.build.publish.plugin.task.appcenter.entity.PrepareResponse
import ru.kode.android.build.publish.plugin.task.appcenter.entity.SendMetaDataResponse
import java.io.File
import java.util.concurrent.TimeUnit

internal class AppCenterUploader(
    private val ownerName: String,
    private val appName: String,
    logger: Logger,
    token: String,
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(HTTP_CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
        .readTimeout(HTTP_CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
        .writeTimeout(HTTP_CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
        .addInterceptor(AttachTokenInterceptor(token))
        .apply {
            val loggingInterceptor = HttpLoggingInterceptor { message -> logger.debug(message) }
            loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
            addNetworkInterceptor(loggingInterceptor)
        }
        .build()

    private val moshi = Moshi.Builder().build()

    private inline fun <reified T> createApi(baseUrl: String): T {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(T::class.java)
    }

    private val api = createApi<AppCenterApi>("https://api.appcenter.ms/v0.1/")

    private var _uploadApi: AppCenterUploadApi? = null

    private val uploadApi: AppCenterUploadApi
        get() {
            return _uploadApi ?: error("upload api is not initialized")
        }

    fun iniUploadApi(uploadDomain: String) {
        _uploadApi = createApi<AppCenterUploadApi>(uploadDomain)
    }

    fun prepareRelease(): PrepareResponse {
        return api.prepareRelease(ownerName, appName).executeOrThrow()
    }

    fun sendMetaData(
        apkFile: File,
        packageAssetId: String,
        encodedToken: String,
    ): SendMetaDataResponse {
        val contentType = "application/vnd.android.package-archive"
        val response = uploadApi.sendMetaData(
            packageAssetId = packageAssetId,
            fileName = apkFile.name,
            fileSize = apkFile.length(),
            encodedToken = encodedToken,
            contentType = contentType,
        ).executeOrThrow()
        if (response.status_code != "Success") {
            throw AppCenterException("send meta data terminated with ${response.status_code}")
        }
        return response
    }

    fun uploadChunk(
        packageAssetId: String,
        encodedToken: String,
        chunkNumber: Int,
        request: ChunkRequestBody,
    ) {
        return uploadApi.uploadChunk(packageAssetId, encodedToken, chunkNumber, request)
            .executeOrThrow()
    }

    fun sendUploadIsFinished(
        packageAssetId: String,
        encodedToken: String,
    ) {
        uploadApi.sendUploadIsFinished(packageAssetId, encodedToken).executeOrThrow()
    }

    fun commit(preparedUploadId: String) {
        api.commit(ownerName, appName, preparedUploadId, CommitRequest(preparedUploadId))
            .executeOrThrow()
    }

    fun waitingReadyToBePublished(preparedUploadId: String): GetUploadResponse {
        var requestCount = 0
        var response: GetUploadResponse
        do {
            response = getUpload(preparedUploadId)
            Thread.sleep(DELAY_CHECK_MS)
            requestCount++
            require(requestCount <= MAX_CHECK) { "Cannot fetch upload status" }
        } while (response.upload_status == "readyToBePublished")
        return response
    }

    private fun getUpload(preparedUploadId: String): GetUploadResponse {
        return api.getUpload(ownerName, appName, preparedUploadId).executeOrThrow()
    }

    fun distribute(
        releaseId: String,
        distributionGroups: Set<String>,
        releaseNotes: String,
    ) {
        val request = DistributeRequest(
            destinations = distributionGroups.map { DistributeRequest.Destination(it) },
            release_notes = releaseNotes,
        )
        api.distribute(ownerName, appName, releaseId, request).executeOrThrow()
    }
}

private class AttachTokenInterceptor(
    private val token: String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val originalRequest = chain.request()
        val newRequest = originalRequest.newBuilder()
            .addHeader(name = "Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .addHeader(name = "X-API-Token", token)
            .build()
        return chain.proceed(newRequest)
    }
}

private fun <T> Call<T>.executeOrThrow() = execute().bodyOrThrow()

private fun <T> Response<T>.bodyOrThrow() = successOrThrow()!!

private fun <T> Response<T>.successOrThrow() =
    if (isSuccessful) {
        body()
    } else {
        throw AppCenterException(
            "App center upload error, code=${code()}, reason=${errorBody()?.string()}",
        )
    }

internal class AppCenterException(override val message: String) : Throwable(message)

private const val HTTP_CONNECT_TIMEOUT_SEC = 60L
private const val DELAY_CHECK_MS = 1000L
private const val MAX_CHECK = 20
