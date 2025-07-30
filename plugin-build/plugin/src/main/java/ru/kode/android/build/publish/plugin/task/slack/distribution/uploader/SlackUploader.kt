package ru.kode.android.build.publish.plugin.task.slack.distribution.uploader

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import okhttp3.Interceptor
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.gradle.api.logging.Logger
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import ru.kode.android.build.publish.plugin.task.slack.distribution.api.SlackApi
import ru.kode.android.build.publish.plugin.task.slack.distribution.entity.UploadingFileRequest
import ru.kode.android.build.publish.plugin.core.util.UploadException
import ru.kode.android.build.publish.plugin.core.util.addProxyIfAvailable
import ru.kode.android.build.publish.plugin.core.util.executeOrThrow
import ru.kode.android.build.publish.plugin.core.util.createPartFromString
import java.io.File
import java.util.concurrent.TimeUnit

internal class SlackUploader(logger: Logger, token: String) {
    private val client =
        OkHttpClient.Builder()
            .connectTimeout(HTTP_CONNECT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
            .readTimeout(HTTP_CONNECT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
            .writeTimeout(HTTP_CONNECT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
            .addInterceptor(AttachTokenInterceptor(token))
            .addProxyIfAvailable()
            .apply {
                val loggingInterceptor = HttpLoggingInterceptor { message -> logger.info(message) }
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

    private val api = createApi<SlackApi>("https://slack.com/api/")

    fun upload(
        baseOutputFileName: String,
        buildName: String,
        file: File,
        channels: Set<String>,
    ) {
        val getUrlResponse =
            api.getUploadUrl(
                fileName = file.name,
                length = file.length(),
            )
                .executeOrThrow()
        if (getUrlResponse.ok) {
            val url = requireNotNull(getUrlResponse.uploadUrl)
            val fileId = requireNotNull(getUrlResponse.fileId)
            val filePart =
                MultipartBody.Part.createFormData(
                    "file",
                    file.name,
                    file.asRequestBody(),
                )
            api.upload(url, createPartFromString(file.name), filePart).executeOrThrow()
            val filesAdapter = moshi.adapter<List<UploadingFileRequest>>()
            val files = filesAdapter.toJson(listOf(UploadingFileRequest(fileId, file.name)))
            api.completeUploading(
                files = files,
                channels = channels.joinToString(),
                initialComment = "$baseOutputFileName $buildName",
            ).executeOrThrow()
        } else {
            throw UploadException(requireNotNull(getUrlResponse.error))
        }
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
                .addHeader(name = "Authorization", "Bearer $token")
                .build()
        return chain.proceed(newRequest)
    }
}

private const val HTTP_CONNECT_TIMEOUT_MINUTES = 3L
