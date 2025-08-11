package ru.kode.android.build.publish.plugin.telegram.task.distribution.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Url
import ru.kode.android.build.publish.plugin.telegram.task.distribution.entity.TelegramUploadResponse

internal interface TelegramDistributionApi {
    @POST
    @Multipart
    fun upload(
        @Url webhookUrl: String,
        @PartMap params: HashMap<String, RequestBody>,
        @Part filePart: MultipartBody.Part,
    ): Call<TelegramUploadResponse>

    @POST
    @Multipart
    fun uploadAuthorised(
        @Header("Authorization") authorisation: String,
        @Url webhookUrl: String,
        @PartMap params: HashMap<String, RequestBody>,
        @Part filePart: MultipartBody.Part,
    ): Call<TelegramUploadResponse>
}
