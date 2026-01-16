package ru.kode.android.build.publish.plugin.telegram.network.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url
import ru.kode.android.build.publish.plugin.telegram.network.entity.SendMessageRequest
import ru.kode.android.build.publish.plugin.telegram.network.entity.TelegramUpdateResponse

/**
 * Retrofit API for Telegram Bot API endpoints used by the plugin.
 */
internal interface TelegramWebhookApi {
    /**
     * Sends a message to a chat.
     */
    @POST
    fun send(
        @Header("Authorization") authorisation: String?,
        @Url webhookUrl: String,
        @Body body: SendMessageRequest,
    ): Call<Unit>

    /**
     * Fetches updates for a bot.
     */
    @GET
    fun getUpdates(
        @Header("Authorization") authorization: String?,
        @Url webhookUrl: String,
    ): Call<TelegramUpdateResponse>
}
