package ru.kode.android.build.publish.plugin.appcenter.task.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import ru.kode.android.build.publish.plugin.appcenter.task.entity.CommitRequest
import ru.kode.android.build.publish.plugin.appcenter.task.entity.DistributeRequest
import ru.kode.android.build.publish.plugin.appcenter.task.entity.GetUploadResponse
import ru.kode.android.build.publish.plugin.appcenter.task.entity.PrepareReleaseRequest
import ru.kode.android.build.publish.plugin.appcenter.task.entity.PrepareResponse

internal interface AppCenterApi {
    @POST("apps/{ownerName}/{appName}/uploads/releases")
    fun prepareRelease(
        @Path("ownerName") ownerName: String,
        @Path("appName") appName: String,
        @Body request: PrepareReleaseRequest,
    ): Call<PrepareResponse>

    @PATCH("apps/{ownerName}/{appName}/uploads/releases/{preparedUploadId}")
    fun commit(
        @Path("ownerName") ownerName: String,
        @Path("appName") appName: String,
        @Path("preparedUploadId") preparedUploadId: String,
        @Body request: CommitRequest,
    ): Call<Unit>

    @GET("apps/{ownerName}/{appName}/uploads/releases/{preparedUploadId}")
    fun getUpload(
        @Path("ownerName") ownerName: String,
        @Path("appName") appName: String,
        @Path("preparedUploadId") preparedUploadId: String,
    ): Call<GetUploadResponse>

    @PATCH("apps/{ownerName}/{appName}/releases/{releaseId}")
    fun distribute(
        @Path("ownerName") ownerName: String,
        @Path("appName") appName: String,
        @Path("releaseId") releaseId: String,
        @Body request: DistributeRequest,
    ): Call<Unit>
}
