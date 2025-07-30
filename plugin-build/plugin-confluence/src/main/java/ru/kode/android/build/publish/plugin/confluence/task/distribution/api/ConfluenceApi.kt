package ru.kode.android.build.publish.plugin.confluence.task.distribution.api

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import ru.kode.android.build.publish.plugin.confluence.task.distribution.entity.AddCommentRequest

interface ConfluenceApi {
    @Multipart
    @POST("rest/api/content/{pageId}/child/attachment")
    fun uploadAttachment(
        @Path("pageId") pageId: String,
        @Header("Authorization") authHeader: String,
        @Header("X-Atlassian-Token") atlassianToken: String = "no-check",
        @Part file: MultipartBody.Part,
    ): Call<Unit>

    @POST("rest/api/content")
    fun addComment(
        @Header("Authorization") authHeader: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Body commentRequest: AddCommentRequest,
    ): Call<Unit>
}
