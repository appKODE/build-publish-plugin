package ru.kode.android.build.publish.plugin.telegram.network.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url
import ru.kode.android.build.publish.plugin.telegram.network.entity.TelegramUpdateResponse

internal interface TelegramWebhookApi {
    @POST
    fun send(
        @Header("Authorization") authorisation: String?,
        @Url webhookUrl: String,
    ): Call<Unit>

    @GET
    fun getUpdates(
        @Header("Authorization") authorization: String?,
        @Url webhookUrl: String,
    ): Call<TelegramUpdateResponse>
}
