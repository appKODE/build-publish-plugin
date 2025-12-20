package ru.kode.android.build.publish.plugin.clickup.controller

import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import ru.kode.android.build.publish.plugin.clickup.controller.entity.ClickUpCustomField
import ru.kode.android.build.publish.plugin.clickup.controller.entity.ClickUpList
import ru.kode.android.build.publish.plugin.clickup.controller.entity.ClickUpSpace
import ru.kode.android.build.publish.plugin.clickup.controller.entity.ClickUpTaskFields
import ru.kode.android.build.publish.plugin.clickup.controller.entity.ClickUpTaskTags
import ru.kode.android.build.publish.plugin.clickup.controller.entity.ClickUpTeam
import ru.kode.android.build.publish.plugin.clickup.messages.customFieldClearedMessage
import ru.kode.android.build.publish.plugin.clickup.messages.failedAddFieldMessage
import ru.kode.android.build.publish.plugin.clickup.messages.failedAddTagMessage
import ru.kode.android.build.publish.plugin.clickup.messages.failedToDeleteCustomFieldMessage
import ru.kode.android.build.publish.plugin.clickup.messages.failedToRemoveTagMessage
import ru.kode.android.build.publish.plugin.clickup.messages.listNotFoundForCreateMessage
import ru.kode.android.build.publish.plugin.clickup.messages.listNotFoundForDeleteMessage
import ru.kode.android.build.publish.plugin.clickup.messages.spaceNotFoundForCreateMessage
import ru.kode.android.build.publish.plugin.clickup.messages.tagRemovedMessage
import ru.kode.android.build.publish.plugin.clickup.messages.teamNotFoundForCreateMessage
import ru.kode.android.build.publish.plugin.clickup.messages.teamNotFoundForDeleteMessage
import ru.kode.android.build.publish.plugin.clickup.network.api.ClickUpApi
import ru.kode.android.build.publish.plugin.clickup.network.entity.AddFieldToTaskRequest
import ru.kode.android.build.publish.plugin.clickup.network.entity.ClearCustomFieldRequest
import ru.kode.android.build.publish.plugin.clickup.network.entity.CreateCustomFieldRequest
import ru.kode.android.build.publish.plugin.core.util.executeNoResult
import ru.kode.android.build.publish.plugin.core.util.executeWithResult

internal class ClickUpControllerImpl(
    private val api: ClickUpApi,
    private val logger: Logger,
) : ClickUpController {
    /**
     * Retrieves the ID of a ClickUp custom field.
     *
     * @param workspaceName The name of the workspace where the custom field is located.
     * @param fieldName The name of the custom field.
     * @return The ID of the custom field.
     */
    override fun getOrCreateCustomFieldId(
        workspaceName: String,
        fieldName: String,
    ): String {
        val team =
            getTeams()
                .firstOrNull { it.name.equals(workspaceName, ignoreCase = true) }
                ?: throw GradleException(teamNotFoundForCreateMessage(workspaceName))
        val space =
            getSpaces(team.id).firstOrNull()
                ?: throw GradleException(spaceNotFoundForCreateMessage(team, workspaceName))
        val list =
            getListsInSpace(space.id).firstOrNull()
                ?: throw GradleException(listNotFoundForCreateMessage(team, workspaceName))
        val customField =
            getCustomFields(list.id)
                .firstOrNull { it.name.equals(fieldName, ignoreCase = true) }

        return customField?.id ?: createCustomField(fieldName, list.id)
    }

    /**
     * Deletes a custom field from a list.
     *
     * @param workspaceName The name of the workspace where the custom field is located.
     * @param fieldId The name of the custom field.
     * @throws GradleException If the custom field is not found in the list or if the list is not found in the workspace.
     */
    override fun deleteCustomFieldFromList(
        workspaceName: String,
        fieldId: String,
    ) {
        val team =
            getTeams()
                .firstOrNull { it.name.equals(workspaceName, ignoreCase = true) }
                ?: throw GradleException(teamNotFoundForDeleteMessage(workspaceName))
        val space =
            getSpaces(team.id).firstOrNull()
                ?: throw GradleException(teamNotFoundForDeleteMessage(team, fieldId))
        val list =
            getListsInSpace(space.id).firstOrNull()
                ?: throw GradleException(listNotFoundForDeleteMessage(space, fieldId))
        api.deleteCustomFieldFromList(list.id, fieldId)
            .executeNoResult()
            .onFailure { logger.error(failedToDeleteCustomFieldMessage(fieldId, list), it) }
    }

    /**
     * Adds a tag to a ClickUp task.
     *
     * @param taskId The ID of the ClickUp task to tag
     * @param tagName The name of the tag to add
     *
     * @throws IOException If the network request fails
     * @throws RuntimeException If the API returns an error response
     */
    override fun addTagToTask(
        taskId: String,
        tagName: String,
    ) {
        api.addTagToTask(taskId, tagName)
            .executeWithResult()
            .onFailure { logger.error(failedAddTagMessage(tagName, taskId), it) }
    }

    /**
     * Removes a tag from a ClickUp task.
     *
     * @param taskId The ID of the ClickUp task to remove the tag from
     * @param tagName The name of the tag to remove
     *
     * @throws IOException If the network request fails
     * @throws RuntimeException If the API returns an error response
     */
    override fun removeTag(
        taskId: String,
        tagName: String,
    ) {
        api.removeTag(taskId, tagName)
            .executeNoResult()
            .onFailure { logger.error(failedToRemoveTagMessage(tagName, taskId), it) }
        logger.info(tagRemovedMessage(tagName, taskId))
    }

    /**
     * Retrieves the list of tags associated with a ClickUp task.
     *
     * @param taskId The ID of the ClickUp task to retrieve tags for
     * @return The list of tags associated with the task
     *
     * @throws IOException If the network request fails
     * @throws RuntimeException If the API returns an error response
     */
    override fun getTaskTags(taskId: String): ClickUpTaskTags {
        val response =
            api.getTaskTags(taskId, includeTags = true)
                .executeWithResult()
                .getOrThrow()
        val tags = response.tags.map { it.name }
        return ClickUpTaskTags(id = taskId, tags = tags)
    }

    /**
     * Clears a custom field value for a ClickUp task.
     *
     * @param taskId The ID of the ClickUp task to update
     * @param fieldId The ID of the custom field to clear
     *
     * @throws IOException If the network request fails
     * @throws RuntimeException If the API returns an error response or the field ID is invalid
     */
    override fun clearCustomField(
        taskId: String,
        fieldId: String,
    ) {
        api.clearCustomField(taskId, fieldId, ClearCustomFieldRequest(value = ""))
            .executeNoResult()
            .getOrThrow()
        logger.info(customFieldClearedMessage(fieldId, taskId))
    }

    /**
     * Retrieves the list of custom fields associated with a ClickUp task.
     *
     * @param taskId The ID of the ClickUp task to retrieve custom fields for
     * @return The list of custom fields associated with the task
     *
     * @throws IOException If the network request fails
     * @throws RuntimeException If the API returns an error response
     */
    override fun getTaskFields(taskId: String): ClickUpTaskFields {
        val response =
            api.getTaskFields(taskId, includeFields = true)
                .executeWithResult()
                .getOrThrow()

        val fields =
            response.custom_fields.map { field ->
                ClickUpCustomField(
                    id = field.id,
                    name = field.name,
                    type = field.type,
                    value = field.value,
                )
            }

        return ClickUpTaskFields(
            id = taskId,
            fields = fields,
        )
    }

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
    override fun addFieldToTask(
        taskId: String,
        fieldId: String,
        fieldValue: String,
    ) {
        val request = AddFieldToTaskRequest(value = fieldValue)
        api.addFieldToTask(taskId, fieldId, request)
            .executeNoResult()
            .onFailure { logger.error(failedAddFieldMessage(fieldId, taskId), it) }
    }

    /**
     * Retrieves all ClickUp teams.
     *
     * @return a list of [ClickUpTeam] objects representing the teams.
     */
    private fun getTeams(): List<ClickUpTeam> {
        val response = api.getTeams().executeWithResult().getOrThrow()
        return response.teams.map { team ->
            ClickUpTeam(
                id = team.id,
                name = team.name,
            )
        }
    }

    /**
     * Retrieves all ClickUp spaces for a given team.
     *
     * @param teamId The ID of the team to retrieve spaces for.
     * @return A list of [ClickUpSpace] objects representing the spaces.
     */
    private fun getSpaces(teamId: String): List<ClickUpSpace> {
        val response = api.getSpaces(teamId).executeWithResult().getOrThrow()
        return response.spaces.map { space ->
            ClickUpSpace(
                id = space.id,
                name = space.name,
            )
        }
    }

    private fun createCustomField(
        fieldName: String,
        listId: String,
    ): String {
        val request = CreateCustomFieldRequest(name = fieldName, type = "text")
        return api.createCustomField(listId, request)
            .executeWithResult()
            .getOrThrow()
            .field
            .id
    }

    private fun getListsInSpace(spaceId: String): List<ClickUpList> {
        val response = api.getListsInSpace(spaceId).executeWithResult().getOrThrow()
        return response.lists.map { list ->
            ClickUpList(
                id = list.id,
            )
        }
    }

    /**
     * Retrieves all ClickUp custom fields for a given list.
     *
     * @param listId The ID of the list to retrieve custom fields for.
     * @return A list of [ClickUpCustomField] objects representing the custom fields.
     */
    private fun getCustomFields(listId: String): List<ClickUpCustomField> {
        val response = api.getCustomFields(listId).executeWithResult().getOrThrow()
        return response.fields.map { field ->
            ClickUpCustomField(
                id = field.id,
                name = field.name,
                type = field.type,
                value = null,
            )
        }
    }
}
