package ru.kode.android.build.publish.plugin.task.jira.service

import com.squareup.moshi.Moshi
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.gradle.api.logging.Logger
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import ru.kode.android.build.publish.plugin.task.jira.api.JiraApi
import ru.kode.android.build.publish.plugin.task.jira.entity.AddFixVersionRequest
import ru.kode.android.build.publish.plugin.task.jira.entity.AddLabelRequest
import ru.kode.android.build.publish.plugin.task.jira.entity.CreateVersionRequest
import ru.kode.android.build.publish.plugin.task.jira.entity.SetStatusRequest
import ru.kode.android.build.publish.plugin.util.executeOptionalOrThrow
import java.util.concurrent.TimeUnit

internal class JiraService(
    logger: Logger,
    baseUrl: String,
    username: String,
    password: String
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(HTTP_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(HTTP_CONNECT_TIMEOUT_SECONDS, TimeUnit.MINUTES)
        .writeTimeout(HTTP_CONNECT_TIMEOUT_SECONDS, TimeUnit.MINUTES)
        .addInterceptor(AttachTokenInterceptor(username, password))
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

    private val api = createApi<JiraApi>("$baseUrl/rest/api/latest/")

    fun setStatus(issue: String, statusTransitionId: String) {
        val request = SetStatusRequest(
            transition = SetStatusRequest.Transition(
                id = statusTransitionId
            )
        )
        api.setStatus(issue, request).executeOptionalOrThrow()
    }

    fun addLabel(issue: String, label: String) {
        val request = AddLabelRequest(
            update = AddLabelRequest.Update(
                labels = listOf(AddLabelRequest.Label(label))
            )
        )
        api.addLabel(issue, request).executeOptionalOrThrow()
    }

    fun createVersion(projectId: Long, version: String) {
        val request = CreateVersionRequest(
            name = version,
            projectId = projectId,
        )
        api.createVersion(request).executeOptionalOrThrow()
    }

    fun addFixVersion(issue: String, version: String) {
        val request = AddFixVersionRequest(
            update = AddFixVersionRequest.Update(
                fixVersions = listOf(
                    AddFixVersionRequest.FixVersion(
                        AddFixVersionRequest.FixVersion.Description(
                            name = version
                        )
                    )
                )
            )
        )
        api.addFixVersion(issue, request).executeOptionalOrThrow()
    }
}

private class AttachTokenInterceptor(
    private val username: String,
    private val password: String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val originalRequest = chain.request()
        val newRequest = originalRequest.newBuilder()
            .addHeader(name = "Content-Type", "application/json")
            .addHeader(name = "Authorization", Credentials.basic(username, password))
            .build()
        return chain.proceed(newRequest)
    }
}

private const val HTTP_CONNECT_TIMEOUT_SECONDS = 60L
