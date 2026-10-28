package ru.kode.android.build.publish.plugin.firebase.network.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url
import ru.kode.android.build.publish.plugin.firebase.controller.entity.RemoteConfigTemplate
import ru.kode.android.build.publish.plugin.firebase.network.entity.ListVersionsResponse

internal interface FirebaseRemoteConfigApi {
    @GET
    fun getTemplate(
        @Url url: String,
        @Query("versionNumber") versionNumber: String?,
    ): Call<RemoteConfigTemplate>

    @GET
    fun listVersions(
        @Url url: String,
        @Query("startTime") startTime: String,
        @Query("pageSize") pageSize: Int,
        @Query("pageToken") pageToken: String?,
    ): Call<ListVersionsResponse>
}
