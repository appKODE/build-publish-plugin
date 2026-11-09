package ru.kode.android.build.publish.plugin.vk.network.api

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Url
import ru.kode.android.build.publish.plugin.vk.network.entity.VkDocSaveResult
import ru.kode.android.build.publish.plugin.vk.network.entity.VkResponse
import ru.kode.android.build.publish.plugin.vk.network.entity.VkUploadResult
import ru.kode.android.build.publish.plugin.vk.network.entity.VkUploadServer

/**
 * VK API methods, see https://dev.vk.com/en/method. The access token and API version are
 * added by the client (`Authorization: Bearer` header and `v` query parameter).
 */
internal interface VkApi {
    @FormUrlEncoded
    @POST("docs.getMessagesUploadServer")
    fun getMessagesUploadServer(
        @Field("peer_id") peerId: Long,
        @Field("type") type: String = "doc",
    ): Call<VkResponse<VkUploadServer>>

    /** Upload URL returned by [getMessagesUploadServer]; it is on a separate upload host. */
    @Multipart
    @POST
    fun uploadFile(
        @Url url: String,
        @Part file: MultipartBody.Part,
    ): Call<VkUploadResult>

    @FormUrlEncoded
    @POST("docs.save")
    fun saveDocument(
        @Field("file") file: String,
        @Field("title") title: String,
    ): Call<VkResponse<VkDocSaveResult>>

    @FormUrlEncoded
    @POST("messages.send")
    fun sendMessage(
        @Field("peer_id") peerId: Long,
        @Field("random_id") randomId: Int,
        @Field("message") message: String?,
        @Field("attachment") attachment: String,
    ): Call<VkResponse<Long>>
}
