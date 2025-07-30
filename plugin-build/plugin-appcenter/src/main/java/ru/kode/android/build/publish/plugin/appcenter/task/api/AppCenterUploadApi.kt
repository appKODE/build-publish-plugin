package ru.kode.android.build.publish.plugin.appcenter.task.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import ru.kode.android.build.publish.plugin.appcenter.task.entity.ChunkRequestBody
import ru.kode.android.build.publish.plugin.appcenter.task.entity.SendMetaDataResponse

internal interface AppCenterUploadApi {
    @POST("/upload/set_metadata/{packageAssetId}")
    fun sendMetaData(
        @Path("packageAssetId") packageAssetId: String,
        @Query("file_name") fileName: String,
        @Query("file_size") fileSize: Long,
        @Query("token", encoded = true) encodedToken: String,
        @Query("content_type") contentType: String,
    ): Call<SendMetaDataResponse>

    @POST("/upload/upload_chunk/{packageAssetId}")
    fun uploadChunk(
        @Path("packageAssetId") packageAssetId: String,
        @Query("token", encoded = true) encodedToken: String,
        @Query("block_number") chunkNumber: Int,
        @Body chunk: ChunkRequestBody,
    ): Call<Unit>

    @POST("/upload/finished/{packageAssetId}")
    fun sendUploadIsFinished(
        @Path("packageAssetId") packageAssetId: String,
        @Query("token", encoded = true) encodedToken: String,
    ): Call<Unit>
}
