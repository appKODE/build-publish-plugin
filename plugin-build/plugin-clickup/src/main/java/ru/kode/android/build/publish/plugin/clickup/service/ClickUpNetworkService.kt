package ru.kode.android.build.publish.plugin.clickup.service

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

abstract class ClickUpNetworkService @Inject constructor() : BuildService<ClickUpNetworkService.Params> {

    interface Params : BuildServiceParameters {
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

            }
        )
        apiProperty.set(
            okHttpClientProperty.map{ client ->
                val moshi = Moshi.Builder().build()
                Retrofit.Builder()
                    .baseUrl(API_BASE_URL)
                    .client(client)
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
                    .build()
                    .create(ClickUpApi::class.java)
            }
        )
    }

    private val api: ClickUpApi get() = apiProperty.get()

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

private const val HTTP_CONNECT_TIMEOUT_SECONDS = 60L
