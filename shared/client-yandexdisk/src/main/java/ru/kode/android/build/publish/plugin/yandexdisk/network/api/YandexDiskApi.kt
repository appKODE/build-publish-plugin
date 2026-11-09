package ru.kode.android.build.publish.plugin.yandexdisk.network.api

import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Query
import retrofit2.http.Url
import ru.kode.android.build.publish.plugin.yandexdisk.network.entity.YandexDiskLink
import ru.kode.android.build.publish.plugin.yandexdisk.network.entity.YandexDiskResource

/**
 * Yandex Disk REST API, see https://yandex.ru/dev/disk-api/doc/en/
 */
internal interface YandexDiskApi {
    @PUT("v1/disk/resources")
    fun createFolder(
        @Query("path") path: String,
    ): Call<Unit>

    @GET("v1/disk/resources/upload")
    fun getUploadLink(
        @Query("path") path: String,
        @Query("overwrite") overwrite: Boolean = true,
    ): Call<YandexDiskLink>

    /**
     * Uploads the file body to the URL returned by [getUploadLink]. That URL is on an upload host,
     * not on the REST API host, and does not need the OAuth token.
     */
    @PUT
    fun uploadByHref(
        @Url href: String,
        @Body body: RequestBody,
    ): Call<Unit>

    @GET("v1/disk/resources")
    fun getResource(
        @Query("path") path: String,
        @Query("fields") fields: String = "public_url",
    ): Call<YandexDiskResource>

    @PUT("v1/disk/resources/publish")
    fun publishResource(
        @Query("path") path: String,
    ): Call<Unit>
}
