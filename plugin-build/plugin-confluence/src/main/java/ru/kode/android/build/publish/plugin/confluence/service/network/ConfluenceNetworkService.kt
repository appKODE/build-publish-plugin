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

abstract class ConfluenceNetworkService
    @Inject
    constructor() : BuildService<ConfluenceNetworkService.Params> {
        interface Params : BuildServiceParameters {
            val baseUrl: Property<String>
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

        companion object {
            private val logger: Logger = Logging.getLogger(ConfluenceNetworkService::class.java)
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

private const val HTTP_CONNECT_TIMEOUT_MINUTES = 3L
