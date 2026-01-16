package ru.kode.android.build.publish.plugin.confluence.network.api

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import ru.kode.android.build.publish.plugin.confluence.network.entity.AddCommentRequest
import ru.kode.android.build.publish.plugin.confluence.network.entity.AttachmentResponse
import ru.kode.android.build.publish.plugin.confluence.network.entity.CommentResponse

/**
 * Retrofit API definition for the Confluence REST endpoints used by this plugin.
 */
internal interface ConfluenceApi {
    @Multipart
    @POST("rest/api/content/{pageId}/child/attachment")
    fun uploadAttachment(
        @Path("pageId") pageId: String,
        @Header("X-Atlassian-Token") atlassianToken: String = "no-check",
        @Part file: MultipartBody.Part,
    ): Call<Unit>

    @POST("rest/api/content")
    fun addComment(
        @Header("Content-Type") contentType: String = "application/json",
        @Body commentRequest: AddCommentRequest,
    ): Call<Unit>

    @GET("rest/api/content/{pageId}/child/attachment")
    fun getAttachments(
        @Path("pageId") pageId: String,
        @Query("expand") expand: String = "results",
    ): Call<AttachmentResponse>

    @GET("rest/api/content/{pageId}/child/comment")
    fun getComments(
        @Path("pageId") pageId: String,
        @Query("expand") expand: String = "body.storage",
    ): Call<CommentResponse>

    @DELETE("rest/api/content/{contentId}")
    fun deleteContent(
        @Path("contentId") contentId: String,
    ): Call<Unit>
}
