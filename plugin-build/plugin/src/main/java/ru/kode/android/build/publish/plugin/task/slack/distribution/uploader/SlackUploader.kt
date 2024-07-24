package ru.kode.android.build.publish.plugin.task.slack.distribution.uploader

import com.squareup.moshi.Moshi
import okhttp3.Interceptor
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.gradle.api.logging.Logger
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import ru.kode.android.build.publish.plugin.task.slack.distribution.api.SlackApi
import ru.kode.android.build.publish.plugin.util.UploadException
import ru.kode.android.build.publish.plugin.util.executeOrThrow
import java.io.File
import java.util.concurrent.TimeUnit

internal class SlackUploader(logger: Logger, token: String) {
    private val client =
        OkHttpClient.Builder()
            .connectTimeout(HTTP_CONNECT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
            .readTimeout(HTTP_CONNECT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
            .writeTimeout(HTTP_CONNECT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
            .addInterceptor(AttachTokenInterceptor(token))
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
        file: File,
        channels: Set<String>,
        message: String?,
    ) {
        val filePart =
            MultipartBody.Part.createFormData(
                "file",
                file.name,
                file.asRequestBody(),
            )
        val map =
            if (message != null) {
                hashMapOf(
                    "initial_comment" to createPartFromString(message),
                    "channels" to createPartFromString(channels.joinToString()),
                )
            } else {
                hashMapOf(
                    "channels" to createPartFromString(channels.joinToString()),
                )
            }
        val response = api.upload(map, filePart).executeOrThrow()
        if (!response.ok) {
            throw UploadException("slack uploading failed ${response.error}")
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

fun createPartFromString(value: String): RequestBody {
    return value.toRequestBody(MultipartBody.FORM)
}

private const val HTTP_CONNECT_TIMEOUT_MINUTES = 3L
