package ru.kode.android.build.publish.plugin.slack.task.changelog.sender.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Url
import ru.kode.android.build.publish.plugin.slack.task.changelog.entity.SlackChangelogBody

internal interface SlackWebhookSenderApi {
    @Headers("Content-Type:application/json")
    @POST
    fun send(
        @Url webhookUrl: String,
        @Body body: SlackChangelogBody,
    ): Call<Unit>
}
