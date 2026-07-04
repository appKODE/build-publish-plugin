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

private const val API_V2 = "v2/"
private const val QUERY_INCLUDE_TAGS = "include_tags"
private const val QUERY_INCLUDE_FIELDS = "include_fields"

/**
 * Retrofit API definition for the ClickUp REST endpoints used by this plugin.
 */
internal interface ClickUpApi {
    @POST(API_V2 + "list/{list_id}/field")
    fun createCustomField(
        @Path("list_id") listId: String,
        @Body request: CreateCustomFieldRequest,
    ): Call<CreateCustomFieldResponse>

    @DELETE(API_V2 + "list/{list_id}/field/{field_id}")
    fun deleteCustomFieldFromList(
        @Path("list_id") listId: String,
        @Path("field_id") fieldId: String,
    ): Call<Unit>

    @GET(API_V2 + "team")
    fun getTeams(): Call<GetTeamsResponse>

    @GET(API_V2 + "team/{team_id}/space")
    fun getSpaces(
        @Path("team_id") teamId: String,
    ): Call<GetSpacesResponse>

    @GET(API_V2 + "space/{space_id}/list")
    fun getListsInSpace(
        @Path("space_id") spaceId: String,
    ): Call<GetListsInSpaceResponse>

    @POST(API_V2 + "task/{task_id}/tag/{tag_name}")
    fun addTagToTask(
        @Path("task_id") taskId: String,
        @Path("tag_name") tagName: String,
    ): Call<Unit>

    @DELETE(API_V2 + "task/{task_id}/tag/{tag_name}")
    fun removeTag(
        @Path("task_id") taskId: String,
        @Path("tag_name") tagName: String,
    ): Call<Unit>

    @GET(API_V2 + "task/{task_id}")
    fun getTaskTags(
        @Path("task_id") taskId: String,
        @Query(QUERY_INCLUDE_TAGS) includeTags: Boolean = true,
    ): Call<GetTaskResponse>

    @GET(API_V2 + "task/{task_id}")
    fun getTaskFields(
        @Path("task_id") taskId: String,
        @Query(QUERY_INCLUDE_FIELDS) includeFields: Boolean = true,
    ): Call<GetTaskResponse>

    @POST(API_V2 + "task/{task_id}/field/{field_id}")
    fun addFieldToTask(
        @Path("task_id") taskId: String,
        @Path("field_id") fieldId: String,
        @Body request: AddFieldToTaskRequest,
    ): Call<Unit>

    @GET(API_V2 + "list/{list_id}/field")
    fun getCustomFields(
        @Path("list_id") listId: String,
    ): Call<GetCustomFieldsResponse>

    @POST(API_V2 + "task/{task_id}/field/{field_id}")
    fun clearCustomField(
        @Path("task_id") taskId: String,
        @Path("field_id") fieldId: String,
        @Body request: ClearCustomFieldRequest,
    ): Call<Unit>
}
