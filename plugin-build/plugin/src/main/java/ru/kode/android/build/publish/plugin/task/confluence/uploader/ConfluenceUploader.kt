package ru.kode.android.build.publish.plugin.task.confluence.uploader

import com.squareup.moshi.Moshi
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.gradle.api.logging.Logger
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import ru.kode.android.build.publish.plugin.task.confluence.api.ConfluenceApi
import ru.kode.android.build.publish.plugin.task.confluence.entity.AddCommentRequest
import ru.kode.android.build.publish.plugin.task.confluence.entity.Body
import ru.kode.android.build.publish.plugin.task.confluence.entity.Container
import ru.kode.android.build.publish.plugin.task.confluence.entity.Storage
import ru.kode.android.build.publish.plugin.util.executeOrThrow
import java.io.File
import java.util.Base64
import java.util.concurrent.TimeUnit

internal class ConfluenceUploader(logger: Logger) {
    private val client =
        OkHttpClient.Builder()
            .connectTimeout(HTTP_CONNECT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
            .readTimeout(HTTP_CONNECT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
            .writeTimeout(HTTP_CONNECT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
            .apply {
                val loggingInterceptor = HttpLoggingInterceptor { message -> logger.info(message) }
                loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
                addNetworkInterceptor(loggingInterceptor)
            }
            .build()

    private val moshi = Moshi.Builder().build()

    private val api =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ConfluenceApi::class.java)

    fun uploadFile(
        username: String,
        password: String,
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
            authHeader = getAuthHeader(username = username, password = password),
        ).executeOrThrow()
    }

    fun addComment(
        username: String,
        password: String,
        pageId: String,
        fileName: String,
    ) {
        val comment = "<a href=\"$BASE_URL/download/attachments/$pageId/$fileName\">$fileName</a>"
        api.addComment(
            commentRequest =
                AddCommentRequest(
                    container = Container(pageId),
                    body = Body(Storage(comment)),
                ),
            authHeader = getAuthHeader(username = username, password = password),
        ).executeOrThrow()
    }

    private fun getAuthHeader(
        username: String,
        password: String,
    ): String {
        val credentials = "$username:$password"
        val encodedCredentials = Base64.getEncoder().encodeToString(credentials.toByteArray())
        return "Basic $encodedCredentials"
    }
}

private const val HTTP_CONNECT_TIMEOUT_MINUTES = 3L
private const val BASE_URL = "https://confa.kode.ru"
