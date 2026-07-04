package ru.kode.android.build.publish.plugin.jira.controller

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import ru.kode.android.build.publish.plugin.jira.controller.entity.JiraFixVersion
import ru.kode.android.build.publish.plugin.jira.controller.entity.JiraIssueStatus

class JiraControllerActionsTest {
    private class RecordingJiraController(
        private val transitionId: String? = "31",
    ) : JiraController {
        val calls = mutableListOf<String>()

        override fun getStatusTransitionId(
            projectKey: String,
            statusName: String,
            issues: List<String>,
        ): String? {
            calls += "getStatusTransitionId:$projectKey:$statusName"
            return transitionId
        }

        override fun setIssueStatus(
            issue: String,
            statusTransitionId: String,
        ) {
            calls += "setIssueStatus:$issue:$statusTransitionId"
        }

        override fun getIssueStatus(issue: String): JiraIssueStatus? = null

        override fun getProjectAvailableStatuses(projectKey: String): List<JiraIssueStatus> = emptyList()

        override fun getProjectId(projectKey: String): Long {
            calls += "getProjectId:$projectKey"
            return PROJECT_ID
        }

        override fun addIssueLabel(
            issue: String,
            label: String,
        ) {
            calls += "addIssueLabel:$issue:$label"
        }

        override fun removeIssueLabel(
            issue: String,
            label: String,
        ) = error("unused")

        override fun getIssueLabels(issue: String): List<String> = emptyList()

        override fun createProjectVersion(
            projectId: Long,
            version: String,
        ) {
            calls += "createProjectVersion:$projectId:$version"
        }

        override fun removeProjectVersion(versionId: String) = error("unused")

        override fun getProjectVersions(projectKey: String): List<JiraFixVersion> = emptyList()

        override fun addIssueFixVersion(
            issue: String,
            version: String,
        ) {
            calls += "addIssueFixVersion:$issue:$version"
        }

        override fun removeIssueFixVersion(
            issue: String,
            version: String,
        ) = error("unused")

        override fun getIssueFixVersions(issue: String): List<JiraFixVersion> = emptyList()

        private companion object {
            const val PROJECT_ID = 42L
        }
    }

    @Test
    fun `addLabelToIssues adds label to every issue`() {
        val controller = RecordingJiraController()
        controller.addLabelToIssues(label = "release", issues = listOf("PROJ-1", "PROJ-2"))
        assertEquals(
            listOf("addIssueLabel:PROJ-1:release", "addIssueLabel:PROJ-2:release"),
            controller.calls,
        )
    }

    @Test
    fun `addFixVersionToIssues uppercases project key and creates version once`() {
        val controller = RecordingJiraController()
        controller.addFixVersionToIssues(projectKey = "proj", version = "1.0.0", issues = listOf("PROJ-1", "PROJ-2"))
        assertEquals(
            listOf(
                "getProjectId:PROJ",
                "createProjectVersion:42:1.0.0",
                "addIssueFixVersion:PROJ-1:1.0.0",
                "addIssueFixVersion:PROJ-2:1.0.0",
            ),
            controller.calls,
        )
    }

    @Test
    fun `transitionIssues uppercases project key and transitions every issue`() {
        val controller = RecordingJiraController(transitionId = "31")
        controller.transitionIssues(projectKey = "proj", transitionName = "Done", issues = listOf("PROJ-1", "PROJ-2"))
        assertEquals(
            listOf(
                "getStatusTransitionId:PROJ:Done",
                "setIssueStatus:PROJ-1:31",
                "setIssueStatus:PROJ-2:31",
            ),
            controller.calls,
        )
    }

    @Test
    fun `transitionIssues throws when transition is not found`() {
        val controller = RecordingJiraController(transitionId = null)
        assertThrows(IllegalStateException::class.java) {
            controller.transitionIssues(projectKey = "proj", transitionName = "Nope", issues = listOf("PROJ-1"))
        }
    }
}
