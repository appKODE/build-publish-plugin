package ru.kode.android.build.publish.plugin.telegram.task.changelog.api

import retrofit2.Call
import retrofit2.http.POST
import retrofit2.http.Url

internal interface TelegramWebhookSenderApi {
    @POST
    fun send(
        @Url webhookUrl: String,
    ): Call<Unit>
}
