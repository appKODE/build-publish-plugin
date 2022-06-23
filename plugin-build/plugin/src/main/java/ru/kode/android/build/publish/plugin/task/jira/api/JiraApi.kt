package ru.kode.android.build.publish.plugin.task.jira.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.PUT
import retrofit2.http.Path
import ru.kode.android.build.publish.plugin.task.jira.entity.AddLabelRequest

internal interface JiraApi {

    @PUT("issue/{issueNumber}")
    fun addLabel(
        @Path("issueNumber") issueNumber: String,
        @Body request: AddLabelRequest,
    ): Call<Unit>
}
