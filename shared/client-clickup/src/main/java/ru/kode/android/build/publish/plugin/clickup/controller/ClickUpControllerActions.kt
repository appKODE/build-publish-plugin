package ru.kode.android.build.publish.plugin.clickup.controller

import ru.kode.android.build.publish.plugin.clickup.messages.addingFixVersionToTasksMessage
import ru.kode.android.build.publish.plugin.clickup.messages.addingTagToTasksMessage

/**
 * High-level, task-oriented ClickUp operations shared by the plugin's standalone tasks
 * and the aggregated sender plugin. Keeping the orchestration here guarantees both
 * entry points execute identical logic (no copy-paste drift).
 */
fun ClickUpController.addTagToTasks(
    tagName: String,
    taskIds: Collection<String>,
    log: (String) -> Unit = {},
) {
    log(addingTagToTasksMessage(tagName, taskIds.size))
    taskIds.forEach { taskId -> addTagToTask(taskId, tagName) }
}

fun ClickUpController.addFixVersionToTasks(
    workspaceName: String,
    fieldName: String,
    version: String,
    taskIds: Collection<String>,
    log: (String) -> Unit = {},
) {
    log(addingFixVersionToTasksMessage(version, taskIds.size))
    val fieldId = getOrCreateCustomFieldId(workspaceName, fieldName)
    taskIds.forEach { taskId -> addFieldToTask(taskId, fieldId, version) }
}
