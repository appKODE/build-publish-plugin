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

internal interface NextcloudApi {
    @PUT("remote.php/dav/files/{username}/{remoteFilePath}")
    fun uploadFile(
        @Path("username") username: String,
        @Path(value = "remoteFilePath", encoded = true) remoteFilePath: String,
        @Header("X-NC-WebDAV-AutoMkcol") autoMkcol: String = "1",
        @Body body: RequestBody,
    ): Call<Unit>

    @Headers("Content-Length: 0")
    @HTTP(method = "MKCOL", path = "remote.php/dav/files/{username}/{remoteFolderPath}", hasBody = false)
    fun createFolder(
        @Path("username") username: String,
        @Path(value = "remoteFolderPath", encoded = true) remoteFolderPath: String,
    ): Call<Unit>

    @GET("ocs/v2.php/cloud/user")
    fun getCurrentUser(
        @Header("OCS-APIRequest") ocsApiRequest: String = "true",
        @Query("format") format: String = "json",
    ): Call<OcsJsonResponse>

    @FormUrlEncoded
    @POST("ocs/v2.php/apps/files_sharing/api/v1/shares")
    fun createShare(
        @Header("OCS-APIRequest") ocsApiRequest: String = "true",
        @Query("format") format: String = "json",
        @Field("path") path: String,
        @Field("shareType") shareType: Int,
        @Field("shareWith") shareWith: String? = null,
        @Field("permissions") permissions: Int = 1,
    ): Call<OcsJsonResponse>

    @GET("ocs/v2.php/apps/files_sharing/api/v1/shares")
    fun getShares(
        @Header("OCS-APIRequest") ocsApiRequest: String = "true",
        @Query("format") format: String = "json",
        @Query("path") path: String,
        @Query("reshares") reshares: Boolean = true,
    ): Call<OcsJsonResponse>
}
