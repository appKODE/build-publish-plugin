package ru.kode.android.build.publish.plugin.clickup.controller

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.kode.android.build.publish.plugin.clickup.controller.entity.ClickUpTaskFields
import ru.kode.android.build.publish.plugin.clickup.controller.entity.ClickUpTaskTags

class ClickUpControllerActionsTest {
    private class RecordingClickUpController(
        private val fieldId: String = "field-1",
    ) : ClickUpController {
        val calls = mutableListOf<String>()

        override fun addTagToTask(
            taskId: String,
            tagName: String,
        ) {
            calls += "addTagToTask:$taskId:$tagName"
        }

        override fun addFieldToTask(
            taskId: String,
            fieldId: String,
            fieldValue: String,
        ) {
            calls += "addFieldToTask:$taskId:$fieldId:$fieldValue"
        }

        override fun getOrCreateCustomFieldId(
            workspaceName: String,
            fieldName: String,
        ): String {
            calls += "getOrCreateCustomFieldId:$workspaceName:$fieldName"
            return fieldId
        }

        override fun clearCustomField(
            taskId: String,
            fieldId: String,
        ) = error("unused")

        override fun removeTag(
            taskId: String,
            tagName: String,
        ) = error("unused")

        override fun getTaskTags(taskId: String): ClickUpTaskTags = error("unused")

        override fun getTaskFields(taskId: String): ClickUpTaskFields = error("unused")

        override fun getTaskName(
            taskId: String,
            teamId: String?,
        ): String? = null

        override fun getTeamId(workspaceName: String): String? = null

        override fun deleteCustomFieldFromList(
            workspaceName: String,
            fieldId: String,
        ) = error("unused")
    }

    @Test
    fun `addTagToTasks tags every task`() {
        val controller = RecordingClickUpController()
        controller.addTagToTasks(tagName = "release", taskIds = listOf("t1", "t2"))
        assertEquals(
            listOf("addTagToTask:t1:release", "addTagToTask:t2:release"),
            controller.calls,
        )
    }

    @Test
    fun `addFixVersionToTasks resolves field once and sets it on every task`() {
        val controller = RecordingClickUpController(fieldId = "field-1")
        controller.addFixVersionToTasks(
            workspaceName = "ws",
            fieldName = "Fix Version",
            version = "1.0.0",
            taskIds = listOf("t1", "t2"),
        )
        assertEquals(
            listOf(
                "getOrCreateCustomFieldId:ws:Fix Version",
                "addFieldToTask:t1:field-1:1.0.0",
                "addFieldToTask:t2:field-1:1.0.0",
            ),
            controller.calls,
        )
    }
}
