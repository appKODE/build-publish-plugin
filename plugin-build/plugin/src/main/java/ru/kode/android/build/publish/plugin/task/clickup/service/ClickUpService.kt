package ru.kode.android.build.publish.plugin.task.clickup.service

import com.squareup.moshi.Moshi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.gradle.api.logging.Logger
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import ru.kode.android.build.publish.plugin.task.clickup.api.ClickUpApi
import ru.kode.android.build.publish.plugin.task.clickup.entity.AddFieldToTaskRequest
import ru.kode.android.build.publish.plugin.util.executeOptionalOrThrow
import java.util.concurrent.TimeUnit

internal class ClickUpService(
    logger: Logger,
    apiToken: String,
) {
    private val client =
        OkHttpClient.Builder()
            .connectTimeout(HTTP_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(HTTP_CONNECT_TIMEOUT_SECONDS, TimeUnit.MINUTES)
            .writeTimeout(HTTP_CONNECT_TIMEOUT_SECONDS, TimeUnit.MINUTES)
            .addInterceptor(AttachTokenInterceptor(apiToken))
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

    private val api = createApi<ClickUpApi>("https://api.clickup.com/api/")

    fun addTagToTask(
        taskId: String,
        tagName: String,
    ) {
        api.addTagToTask(taskId, tagName).executeOptionalOrThrow()
    }

    fun addFieldToTask(
        taskId: String,
        fieldId: String,
        fieldValue: String,
    ) {
        val request = AddFieldToTaskRequest(value = fieldValue)
        api.addFieldToTask(taskId, fieldId, request).executeOptionalOrThrow()
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

private const val HTTP_CONNECT_TIMEOUT_SECONDS = 60L
