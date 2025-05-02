package ru.kode.android.build.publish.plugin.task.slack.distribution.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query
import retrofit2.http.Url
import ru.kode.android.build.publish.plugin.task.slack.distribution.entity.SlackCompleteUploadUrlResponse
import ru.kode.android.build.publish.plugin.task.slack.distribution.entity.SlackGetUploadUrlResponse

internal interface SlackApi {
    @GET("files.getUploadURLExternal")
    fun getUploadUrl(
        @Query("filename") fileName: String,
        @Query("length") length: Long,
    ): Call<SlackGetUploadUrlResponse>

    @POST
    @Multipart
    fun upload(
        @Url url: String,
        @Part("filename") fileName: RequestBody,
        @Part filePart: MultipartBody.Part,
    ): Call<ResponseBody>

    @FormUrlEncoded
    @POST("files.completeUploadExternal")
    fun completeUploading(
        @Field("files") files: String,
        @Field("channels") channels: String,
        @Field("initial_comment") initialComment: String?,
    ): Call<SlackCompleteUploadUrlResponse>
}
