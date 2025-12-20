package ru.kode.android.build.publish.plugin.clickup.network.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import ru.kode.android.build.publish.plugin.clickup.network.entity.AddFieldToTaskRequest
import ru.kode.android.build.publish.plugin.clickup.network.entity.ClearCustomFieldRequest
import ru.kode.android.build.publish.plugin.clickup.network.entity.CreateCustomFieldRequest
import ru.kode.android.build.publish.plugin.clickup.network.entity.CreateCustomFieldResponse
import ru.kode.android.build.publish.plugin.clickup.network.entity.GetCustomFieldsResponse
import ru.kode.android.build.publish.plugin.clickup.network.entity.GetListsInSpaceResponse
import ru.kode.android.build.publish.plugin.clickup.network.entity.GetSpacesResponse
import ru.kode.android.build.publish.plugin.clickup.network.entity.GetTaskResponse
import ru.kode.android.build.publish.plugin.clickup.network.entity.GetTeamsResponse

internal interface ClickUpApi {
    @POST("v2/list/{list_id}/field")
    fun createCustomField(
        @Path("list_id") listId: String,
        @Body request: CreateCustomFieldRequest,
    ): Call<CreateCustomFieldResponse>

    @DELETE("v2/list/{list_id}/field/{field_id}")
    fun deleteCustomFieldFromList(
        @Path("list_id") listId: String,
        @Path("field_id") fieldId: String,
    ): Call<Unit>

    @GET("v2/team")
    fun getTeams(): Call<GetTeamsResponse>

    @GET("v2/team/{team_id}/space")
    fun getSpaces(
        @Path("team_id") teamId: String,
    ): Call<GetSpacesResponse>

    @GET("v2/space/{space_id}/list")
    fun getListsInSpace(
        @Path("space_id") spaceId: String,
    ): Call<GetListsInSpaceResponse>

    @POST("v2/task/{task_id}/tag/{tag_name}")
    fun addTagToTask(
        @Path("task_id") taskId: String,
        @Path("tag_name") tagName: String,
    ): Call<Unit>

    @DELETE("v2/task/{task_id}/tag/{tag_name}")
    fun removeTag(
        @Path("task_id") taskId: String,
        @Path("tag_name") tagName: String,
    ): Call<Unit>

    @GET("v2/task/{task_id}")
    fun getTaskTags(
        @Path("task_id") taskId: String,
        @Query("include_tags") includeTags: Boolean = true,
    ): Call<GetTaskResponse>

    @GET("v2/task/{task_id}")
    fun getTaskFields(
        @Path("task_id") taskId: String,
        @Query("include_fields") includeFields: Boolean = true,
    ): Call<GetTaskResponse>

    @POST("v2/task/{task_id}/field/{field_id}")
    fun addFieldToTask(
        @Path("task_id") taskId: String,
        @Path("field_id") fieldId: String,
        @Body request: AddFieldToTaskRequest,
    ): Call<Unit>

    @GET("v2/list/{list_id}/field")
    fun getCustomFields(
        @Path("list_id") listId: String,
    ): Call<GetCustomFieldsResponse>

    @POST("v2/task/{task_id}/field/{field_id}")
    fun clearCustomField(
        @Path("task_id") taskId: String,
        @Path("field_id") fieldId: String,
        @Body request: ClearCustomFieldRequest,
    ): Call<Unit>
}
