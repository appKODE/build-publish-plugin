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

private const val MEDIA_TYPE_JSON = "application/json"
private const val HEADER_CONTENT_TYPE = "Content-Type"
private const val HEADER_ATLASSIAN_TOKEN = "X-Atlassian-Token"
private const val ATLASSIAN_TOKEN_NO_CHECK = "no-check"
private const val QUERY_EXPAND = "expand"
private const val CONTENT_PATH = "rest/api/content"

/**
 * Retrofit API definition for the Confluence REST endpoints used by this plugin.
 */
internal interface ConfluenceApi {
    @Multipart
    @POST(CONTENT_PATH + "/{pageId}/child/attachment")
    fun uploadAttachment(
        @Path("pageId") pageId: String,
        @Header(HEADER_ATLASSIAN_TOKEN) atlassianToken: String = ATLASSIAN_TOKEN_NO_CHECK,
        @Part file: MultipartBody.Part,
    ): Call<Unit>

    @POST(CONTENT_PATH)
    fun addComment(
        @Header(HEADER_CONTENT_TYPE) contentType: String = MEDIA_TYPE_JSON,
        @Body commentRequest: AddCommentRequest,
    ): Call<Unit>

    @GET(CONTENT_PATH + "/{pageId}/child/attachment")
    fun getAttachments(
        @Path("pageId") pageId: String,
        @Query(QUERY_EXPAND) expand: String = "results",
    ): Call<AttachmentResponse>

    @GET(CONTENT_PATH + "/{pageId}/child/comment")
    fun getComments(
        @Path("pageId") pageId: String,
        @Query(QUERY_EXPAND) expand: String = "body.storage",
    ): Call<CommentResponse>

    @DELETE(CONTENT_PATH + "/{contentId}")
    fun deleteContent(
        @Path("contentId") contentId: String,
    ): Call<Unit>
}
