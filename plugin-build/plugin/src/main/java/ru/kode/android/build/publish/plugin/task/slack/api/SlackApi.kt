package ru.kode.android.build.publish.plugin.task.slack.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap
import ru.kode.android.build.publish.plugin.task.slack.entity.SlackUploadResponse

internal interface SlackApi {

    @POST("files.upload")
    @Multipart
    fun upload(
        @PartMap params: HashMap<String, RequestBody>,
        @Part filePart: MultipartBody.Part,
    ): Call<SlackUploadResponse>
}
