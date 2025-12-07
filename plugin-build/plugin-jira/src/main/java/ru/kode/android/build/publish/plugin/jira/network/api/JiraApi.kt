package ru.kode.android.build.publish.plugin.jira.network.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import ru.kode.android.build.publish.plugin.jira.network.entity.AddFixVersionRequest
import ru.kode.android.build.publish.plugin.jira.network.entity.AddLabelRequest
import ru.kode.android.build.publish.plugin.jira.network.entity.CreateVersionRequest
import ru.kode.android.build.publish.plugin.jira.network.entity.JiraFixVersion
import ru.kode.android.build.publish.plugin.jira.network.entity.GetFixVersionsResponse
import ru.kode.android.build.publish.plugin.jira.network.entity.GetLabelsResponse
import ru.kode.android.build.publish.plugin.jira.network.entity.GetStatusResponse
import ru.kode.android.build.publish.plugin.jira.network.entity.RemoveFixVersionRequest
import ru.kode.android.build.publish.plugin.jira.network.entity.RemoveLabelRequest
import ru.kode.android.build.publish.plugin.jira.network.entity.SetStatusRequest

internal interface JiraApi {
    @POST("version")
    fun createVersion(
        @Body request: CreateVersionRequest,
    ): Call<Unit>

    @DELETE("version/{versionId}")
    fun deleteVersion(
        @Path("versionId") versionId: String,
        @Query("moveFixIssuesTo") moveFixIssuesTo: Long? = null,
        @Query("moveAffectedIssuesTo") moveAffectedIssuesTo: Long? = null,
    ): Call<Unit>

    @GET("project/{projectKey}/versions")
    fun getProjectVersions(
        @Path("projectKey") projectKey: String
    ): Call<List<JiraFixVersion>>

    @PUT("issue/{issueNumber}")
    fun addLabel(
        @Path("issueNumber") issueNumber: String,
        @Body request: AddLabelRequest,
    ): Call<Unit>

    @PUT("issue/{issueNumber}")
    fun removeLabel(
        @Path("issueNumber") issueNumber: String,
        @Body request: RemoveLabelRequest,
    ): Call<Unit>

    @POST("issue/{issueNumber}/transitions")
    fun setStatus(
        @Path("issueNumber") issueNumber: String,
        @Body request: SetStatusRequest,
    ): Call<Unit>

    @GET("issue/{issueNumber}")
    fun getStatus(
        @Path("issueNumber") issueNumber: String,
        @Query("fields") fields: String = "status",
    ): Call<GetStatusResponse>

    @PUT("issue/{issueNumber}")
    fun addFixVersion(
        @Path("issueNumber") issueNumber: String,
        @Body request: AddFixVersionRequest,
    ): Call<Unit>

    @GET("issue/{issueNumber}")
    fun getLabels(
        @Path("issueNumber") issueNumber: String,
        @Query("fields") fields: String = "labels",
    ): Call<GetLabelsResponse>

    @PUT("issue/{issueNumber}")
    fun removeFixVersion(
        @Path("issueNumber") issueNumber: String,
        @Body request: RemoveFixVersionRequest,
    ): Call<Unit>

    @GET("issue/{issueNumber}")
    fun getFixVersions(
        @Path("issueNumber") issueNumber: String,
        @Query("fields") fields: String = "fixVersions",
    ): Call<GetFixVersionsResponse>
}
