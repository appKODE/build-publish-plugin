package ru.kode.android.build.publish.plugin.task.telegram.distribution.uploader

import com.squareup.moshi.Moshi
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.gradle.api.logging.Logger
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import ru.kode.android.build.publish.plugin.task.slack.distribution.uploader.createPartFromString
import ru.kode.android.build.publish.plugin.task.telegram.distribution.api.TelegramApi
import ru.kode.android.build.publish.plugin.util.UploadException
import ru.kode.android.build.publish.plugin.util.executeOrThrow
import java.io.File
import java.util.concurrent.TimeUnit

internal class TelegramUploader(logger: Logger) {
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
            .baseUrl(STUB_BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(TelegramApi::class.java)

    fun upload(
        webhookUrl: String,
        chatId: String,
        topicId: String?,
        file: File,
    ) {
        val filePart =
            MultipartBody.Part.createFormData(
                "document",
                file.name,
                file.asRequestBody(),
            )
        val params =
            if (topicId != null) {
                hashMapOf(
                    "message_thread_id" to createPartFromString(topicId),
                    "chat_id" to createPartFromString(chatId),
                )
            } else {
                hashMapOf(
                    "chat_id" to createPartFromString(chatId),
                )
            }
        val response = api.upload(webhookUrl, params, filePart).executeOrThrow()
        if (!response.ok) {
            throw UploadException("Telegram uploading failed ${response.error_code}")
        }
    }
}

private const val HTTP_CONNECT_TIMEOUT_MINUTES = 3L
private const val STUB_BASE_URL = "http://localhost/"
