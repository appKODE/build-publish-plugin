package ru.kode.android.build.publish.plugin.task.clickup.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import ru.kode.android.build.publish.plugin.task.clickup.entity.AddFieldToTaskRequest

interface ClickUpApi {
    @POST("v2/task/{task_id}/tag/{tag_name}")
    fun addTagToTask(
        @Path("task_id") taskId: String,
        @Path("tag_name") tagName: String,
    ): Call<Unit>

    @POST("v2/task/{task_id}/field/{field_id}")
    fun addFieldToTask(
        @Path("task_id") taskId: String,
        @Path("field_id") fieldId: String,
        @Body request: AddFieldToTaskRequest,
    ): Call<Unit>
}
