package ru.kode.android.build.publish.plugin.nextcloud.network.api

import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import ru.kode.android.build.publish.plugin.nextcloud.network.entity.OcsJsonResponse

private const val HEADER_OCS_API_REQUEST = "OCS-APIRequest"
private const val OCS_API_REQUEST_ENABLED = "true"
private const val HEADER_CONTENT_LENGTH_ZERO = "Content-Length: 0"
private const val HEADER_WEBDAV_AUTO_MKCOL = "X-NC-WebDAV-AutoMkcol"
private const val WEBDAV_AUTO_MKCOL_ENABLED = "1"
private const val QUERY_FORMAT = "format"
private const val FORMAT_JSON = "json"
private const val DAV_FILES_PATH = "remote.php/dav/files/"

internal interface NextcloudApi {
    @PUT(DAV_FILES_PATH + "{username}/{remoteFilePath}")
    fun uploadFile(
        @Path("username") username: String,
        @Path(value = "remoteFilePath", encoded = true) remoteFilePath: String,
        @Header(HEADER_WEBDAV_AUTO_MKCOL) autoMkcol: String = WEBDAV_AUTO_MKCOL_ENABLED,
        @Body body: RequestBody,
    ): Call<Unit>

    @Headers(HEADER_CONTENT_LENGTH_ZERO)
    @HTTP(method = "MKCOL", path = DAV_FILES_PATH + "{username}/{remoteFolderPath}", hasBody = false)
    fun createFolder(
        @Path("username") username: String,
        @Path(value = "remoteFolderPath", encoded = true) remoteFolderPath: String,
    ): Call<Unit>

    @GET("ocs/v2.php/cloud/user")
    fun getCurrentUser(
        @Header(HEADER_OCS_API_REQUEST) ocsApiRequest: String = OCS_API_REQUEST_ENABLED,
        @Query(QUERY_FORMAT) format: String = FORMAT_JSON,
    ): Call<OcsJsonResponse>

    @FormUrlEncoded
    @POST("ocs/v2.php/apps/files_sharing/api/v1/shares")
    fun createShare(
        @Header(HEADER_OCS_API_REQUEST) ocsApiRequest: String = OCS_API_REQUEST_ENABLED,
        @Query(QUERY_FORMAT) format: String = FORMAT_JSON,
        @Field("path") path: String,
        @Field("shareType") shareType: Int,
        @Field("shareWith") shareWith: String? = null,
        @Field("permissions") permissions: Int = 1,
    ): Call<OcsJsonResponse>

    @GET("ocs/v2.php/apps/files_sharing/api/v1/shares")
    fun getShares(
        @Header(HEADER_OCS_API_REQUEST) ocsApiRequest: String = OCS_API_REQUEST_ENABLED,
        @Query(QUERY_FORMAT) format: String = FORMAT_JSON,
        @Query("path") path: String,
        @Query("reshares") reshares: Boolean = true,
    ): Call<OcsJsonResponse>
}
