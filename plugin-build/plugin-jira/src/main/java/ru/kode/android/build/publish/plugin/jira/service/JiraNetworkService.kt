package ru.kode.android.build.publish.plugin.jira.service

import com.squareup.moshi.Moshi
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import ru.kode.android.build.publish.plugin.core.util.addProxyIfAvailable
import ru.kode.android.build.publish.plugin.core.util.executeOptionalOrThrow
import ru.kode.android.build.publish.plugin.jira.task.automation.api.JiraApi
import ru.kode.android.build.publish.plugin.jira.task.automation.entity.AddFixVersionRequest
import ru.kode.android.build.publish.plugin.jira.task.automation.entity.AddLabelRequest
import ru.kode.android.build.publish.plugin.jira.task.automation.entity.CreateVersionRequest
import ru.kode.android.build.publish.plugin.jira.task.automation.entity.SetStatusRequest
import java.util.concurrent.TimeUnit
import javax.inject.Inject

abstract class JiraNetworkService @Inject constructor() : BuildService<JiraNetworkService.Params> {

    interface Params : BuildServiceParameters {
        val baseUrl: Property<String>
        val username: Property<String>
        val password: Property<String>
    }

    protected abstract val okHttpClientProperty: Property<OkHttpClient>
    protected abstract val apiProperty: Property<JiraApi>

    init {
        okHttpClientProperty.set(
            parameters.username.zip(parameters.password) { username, password ->
                OkHttpClient.Builder()
                    .connectTimeout(HTTP_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .readTimeout(HTTP_CONNECT_TIMEOUT_SECONDS, TimeUnit.MINUTES)
                    .writeTimeout(HTTP_CONNECT_TIMEOUT_SECONDS, TimeUnit.MINUTES)
                    .addInterceptor(AttachTokenInterceptor(username, password))
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
            okHttpClientProperty.zip(parameters.baseUrl) { client, baseUrl ->
                val moshi = Moshi.Builder().build()
                Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(client)
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
                    .build()
                    .create(JiraApi::class.java)
            }
        )
    }

    val api: JiraApi
        get() = apiProperty.get().also {
            println("baseurl ${parameters.baseUrl.get()}, login ${parameters.username.get()}")
        }

    fun setStatus(
        issue: String,
        statusTransitionId: String,
    ) {
        val request =
            SetStatusRequest(
                transition =
                    SetStatusRequest.Transition(
                        id = statusTransitionId,
                    ),
            )
        api.setStatus(issue, request).executeOptionalOrThrow()
    }

    fun addLabel(
        issue: String,
        label: String,
    ) {
        val request =
            AddLabelRequest(
                update =
                    AddLabelRequest.Update(
                        labels = listOf(AddLabelRequest.Label(label)),
                    ),
            )
        api.addLabel(issue, request).executeOptionalOrThrow()
    }

    fun createVersion(
        projectId: Long,
        version: String,
    ) {
        val request =
            CreateVersionRequest(
                name = version,
                projectId = projectId,
            )
        api.createVersion(request).executeOptionalOrThrow()
    }

    fun addFixVersion(
        issue: String,
        version: String,
    ) {
        val request =
            AddFixVersionRequest(
                update =
                    AddFixVersionRequest.Update(
                        fixVersions =
                            listOf(
                                AddFixVersionRequest.FixVersion(
                                    AddFixVersionRequest.FixVersion.Description(
                                        name = version,
                                    ),
                                ),
                            ),
                    ),
            )
        api.addFixVersion(issue, request).executeOptionalOrThrow()
    }

    companion object {
        private val logger: Logger = Logging.getLogger(JiraNetworkService::class.java)
    }
}

private class AttachTokenInterceptor(
    private val username: String,
    private val password: String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val originalRequest = chain.request()
        val newRequest =
            originalRequest.newBuilder()
                .addHeader(name = "Content-Type", "application/json")
                .addHeader(name = "Authorization", Credentials.basic(username, password))
                .build()
        return chain.proceed(newRequest)
    }
}

private const val HTTP_CONNECT_TIMEOUT_SECONDS = 60L
