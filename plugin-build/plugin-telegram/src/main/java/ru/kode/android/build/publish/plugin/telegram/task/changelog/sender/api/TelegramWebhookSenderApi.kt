package ru.kode.android.build.publish.plugin.telegram.task.changelog.sender.api

import retrofit2.Call
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

internal interface TelegramWebhookSenderApi {
    @POST
    fun send(
        @Header("Authorization") authHeader: String?,
        @Url webhookUrl: String,
    ): Call<Unit>
}
