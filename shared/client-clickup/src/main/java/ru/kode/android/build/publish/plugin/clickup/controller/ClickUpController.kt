package ru.kode.android.build.publish.plugin.clickup.controller

import ru.kode.android.build.publish.plugin.clickup.controller.entity.ClickUpTaskFields
import ru.kode.android.build.publish.plugin.clickup.controller.entity.ClickUpTaskTags

/**
 * High-level ClickUp operations used by tasks in this plugin.
 */
interface ClickUpController {
    /**
     * Adds a tag to a ClickUp task.
     *
     * @param taskId The ID of the ClickUp task to tag
     * @param tagName The name of the tag to add
     *
     * @throws IOException If the network request fails
     * @throws RuntimeException If the API returns an error response
     */
    fun addTagToTask(
        taskId: String,
        tagName: String,
    )

    /**
     * Adds or updates a custom field value for a ClickUp task.
     *
     * @param taskId The ID of the ClickUp task to update
     * @param fieldId The ID of the custom field to set
     * @param fieldValue The value to set for the custom field
     *
     * @throws IOException If the network request fails
     * @throws RuntimeException If the API returns an error response or the field ID is invalid
     */
    fun addFieldToTask(
        taskId: String,
        fieldId: String,
        fieldValue: String,
    )

    /**
     * Retrieves the ID of a ClickUp custom field.
     *
     * @param workspaceName The name of the workspace where the custom field is located.
     * @param fieldName The name of the custom field.
     * @return The ID of the custom field.
     */
    fun getOrCreateCustomFieldId(
        workspaceName: String,
        fieldName: String,
    ): String

    /**
     * Clears a custom field value for a ClickUp task.
     *
     * @param taskId The ID of the ClickUp task to update
     * @param fieldId The ID of the custom field to clear
     *
     * @throws IOException If the network request fails
     * @throws RuntimeException If the API returns an error response or the field ID is invalid
     */
    fun clearCustomField(
        taskId: String,
        fieldId: String,
    )

    /**
     * Removes a tag from a ClickUp task.
     *
     * @param taskId The ID of the ClickUp task to remove the tag from
     * @param tagName The name of the tag to remove
     *
     * @throws IOException If the network request fails
     * @throws RuntimeException If the API returns an error response
     */
    fun removeTag(
        taskId: String,
        tagName: String,
    )

    /**
     * Retrieves the list of tags associated with a ClickUp task.
     *
     * @param taskId The ID of the ClickUp task to retrieve tags for
     * @return The list of tags associated with the task
     *
     * @throws IOException If the network request fails
     * @throws RuntimeException If the API returns an error response
     */
    fun getTaskTags(taskId: String): ClickUpTaskTags

    /**
     * Retrieves the list of custom fields associated with a ClickUp task.
     *
     * @param taskId The ID of the ClickUp task to retrieve custom fields for
     * @return The list of custom fields associated with the task
     *
     * @throws IOException If the network request fails
     * @throws RuntimeException If the API returns an error response
     */
    fun getTaskFields(taskId: String): ClickUpTaskFields

    /**
     * Retrieves the name (title) of a ClickUp task.
     *
     * @param taskId The task id. A native ClickUp id when [teamId] is `null`, or a custom task id
     *   (e.g. `PREFIX-123`) when [teamId] is provided.
     * @param teamId When non-null, the task is resolved as a custom task id scoped to this ClickUp team
     *   (workspace) via `custom_task_ids=true&team_id=<teamId>`; when `null`, [taskId] is treated as a
     *   native id.
     * @return The task name, or `null` when it cannot be retrieved (unknown task or request error).
     */
    fun getTaskName(
        taskId: String,
        teamId: String? = null,
    ): String?

    /**
     * Resolves a ClickUp workspace (team) name to its team id, or `null` when no team with that name is
     * reachable by the current token. Used to scope custom-task-id lookups to the right workspace.
     *
     * @param workspaceName The ClickUp workspace (team) name.
     */
    fun getTeamId(workspaceName: String): String?

    /**
     * Deletes a custom field from a list.
     *
     * @param workspaceName The name of the workspace where the custom field is located.
     * @param fieldId The id of the custom field.
     * @throws IllegalStateException If the custom field is not found in the list or if the list is not found in the workspace.
     */
    fun deleteCustomFieldFromList(
        workspaceName: String,
        fieldId: String,
    )
}
