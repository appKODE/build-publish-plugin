package ru.kode.android.build.publish.plugin.jira.task.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import ru.kode.android.build.publish.plugin.jira.task.entity.AddFixVersionRequest
import ru.kode.android.build.publish.plugin.jira.task.entity.AddLabelRequest
import ru.kode.android.build.publish.plugin.jira.task.entity.CreateVersionRequest
import ru.kode.android.build.publish.plugin.jira.task.entity.SetStatusRequest

internal interface JiraApi {
    @POST("version")
    fun createVersion(
        @Body request: CreateVersionRequest,
    ): Call<Unit>

    @PUT("issue/{issueNumber}")
    fun addLabel(
        @Path("issueNumber") issueNumber: String,
        @Body request: AddLabelRequest,
    ): Call<Unit>

    @POST("issue/{issueNumber}/transitions")
    fun setStatus(
        @Path("issueNumber") issueNumber: String,
        @Body request: SetStatusRequest,
    ): Call<Unit>

    @PUT("issue/{issueNumber}")
    fun addFixVersion(
        @Path("issueNumber") issueNumber: String,
        @Body request: AddFixVersionRequest,
    ): Call<Unit>
}
