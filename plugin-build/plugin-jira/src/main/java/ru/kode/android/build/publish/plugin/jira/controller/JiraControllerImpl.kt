package ru.kode.android.build.publish.plugin.jira.controller

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import ru.kode.android.build.publish.plugin.core.util.executeWithResult
import ru.kode.android.build.publish.plugin.core.util.executeNoResult
import ru.kode.android.build.publish.plugin.jira.controller.entity.JiraFixVersion
import ru.kode.android.build.publish.plugin.jira.controller.entity.JiraIssueStatus
import ru.kode.android.build.publish.plugin.jira.network.api.JiraApi
import ru.kode.android.build.publish.plugin.jira.network.entity.AddFixVersionRequest
import ru.kode.android.build.publish.plugin.jira.network.entity.AddLabelRequest
import ru.kode.android.build.publish.plugin.jira.network.entity.CreateVersionRequest
import ru.kode.android.build.publish.plugin.jira.network.entity.RemoveFixVersionRequest
import ru.kode.android.build.publish.plugin.jira.network.entity.RemoveLabelRequest
import ru.kode.android.build.publish.plugin.jira.network.entity.SetStatusRequest

/**
 * Controller for interacting with the Jira API.
 *
 * @param api The Jira API implementation
 */
internal class JiraControllerImpl(
    private val api: JiraApi,
    private val logger: Logger,
) : JiraController {

    /**
     * Transitions a Jira issue to a new status.
     *
     * @param issue The issue key (e.g., "PROJ-123")
     * @param statusTransitionId The ID of the status transition to execute
     *
     * @throws IOException If the network request fails
     * @throws JiraApiException If the Jira API returns an error
     */
    override fun setIssueStatus(
        issue: String,
        statusTransitionId: String,
    ) {
        val request =
            SetStatusRequest(
                transition =
                    SetStatusRequest.Transition(
                        id = statusTransitionId,
                    ),
            )
        api.setStatus(issue, request).executeNoResult()
    }

    /**
     * Retrieves the current status of a Jira issue, including both the name and ID.
     *
     * This method calls:
     * `GET /rest/api/2/issue/{issue}?fields=status`
     *
     * The Jira API response includes the status ID, name, description, and category.
     * This function extracts only the ID and name.
     *
     * Example returned values:
     * - ID: "3", Name: "In Progress"
     * - ID: "1", Name: "To Do"
     *
     * If the status cannot be retrieved (null or missing), the function
     * returns ID `"unknown"` and name `"Unknown"`.
     *
     * @param issue The Jira issue key (e.g., `"PROJ-123"`).
     *
     * @return A `Pair` of `(statusId, statusName)`.
     *
     * @throws IOException If the HTTP request fails.
     * @throws JiraApiException If Jira responds with an error code or unexpected result.
     */
    override fun getIssueStatus(issue: String): JiraIssueStatus? {
        val status = api.getStatus(issue)
            .executeWithResult()
            .getOrNull()
            ?.fields
            ?.status

        return if (status != null) {
            JiraIssueStatus(
                id = status.id,
                name = status.name
            )
        } else {
            null
        }
    }

    /**
     * Adds a label to a Jira issue.
     *
     * @param issue The issue key (e.g., "PROJ-123")
     * @param label The label to add
     *
     * @throws IOException If the network request fails
     * @throws JiraApiException If the Jira API returns an error
     */
    override fun addIssueLabel(
        issue: String,
        label: String,
    ) {
        val request =
            AddLabelRequest(
                update =
                    AddLabelRequest.Update(
                        labels = listOf(AddLabelRequest.Label(label)),
                    ),
            )
        api
            .addLabel(issue, request)
            .executeNoResult()
            .onFailure { logger.error("Failed to add label for $issue", it) }
    }

    /**
     * Removes a label from a Jira issue.
     *
     * @param issue The issue key (e.g., "PROJ-123")
     * @param label The label to remove
     *
     * @throws IOException If the network request fails
     * @throws JiraApiException If the Jira API returns an error
     */
    override fun removeIssueLabel(
        issue: String,
        label: String,
    ) {
        val request =
            RemoveLabelRequest(
                update =
                    RemoveLabelRequest.Update(
                        labels = listOf(RemoveLabelRequest.LabelRemove(label)),
                    ),
            )

        api
            .removeLabel(issue, request)
            .executeNoResult()
            .getOrNull()
    }

    /**
     * Retrieves labels of a Jira issue.
     *
     * @param issue The issue key (e.g. "PROJ-123")
     * @return List of labels attached to the issue
     *
     * @throws IOException If the network request fails
     * @throws JiraApiException If the Jira API returns an error
     */
    override fun getIssueLabels(issue: String): List<String> {
        val response =
            api.getLabels(issue)
                .executeWithResult()
                .getOrNull()

        return response?.fields?.labels ?: emptyList()
    }

    /**
     * Creates a new version in a Jira project.
     *
     * @param projectId The ID of the Jira project
     * @param version The version name to create
     *
     * @throws IOException If the network request fails
     * @throws JiraApiException If the Jira API returns an error or version already exists
     */
    override fun createProjectVersion(
        projectId: Long,
        version: String,
    ) {
        val request =
            CreateVersionRequest(
                name = version,
                projectId = projectId,
            )
        api.createVersion(request).executeNoResult()
    }

    /**
     * Removes a version from a Jira project.
     *
     * @param versionId ID of the version to delete
     *
     * @throws IOException If request fails
     * @throws JiraApiException If Jira API returns an error
     */
    override fun removeProjectVersion(
        versionId: String,
    ) {
        api.deleteVersion(
            versionId = versionId,
            moveFixIssuesTo = null,
            moveAffectedIssuesTo = null,
        ).executeNoResult()
    }

    /**
     * Retrieves all versions of a Jira project.
     *
     * @param projectKey The key of the Jira project
     * @return List of project versions
     *
     * @throws IOException If the network request fails
     * @throws JiraApiException If the Jira API returns an error
     */
    override fun getProjectVersions(projectKey: String): List<JiraFixVersion> {
        return api.getProjectVersions(projectKey)
            .executeWithResult()
            .getOrThrow()
            .map {
                JiraFixVersion(
                    id = it.id,
                    name = it.name
                )
            }
    }

    /**
     * Adds a fix version to a Jira issue.
     *
     * @param issue The issue key (e.g., "PROJ-123")
     * @param version The version to add as a fix version
     *
     * @throws IOException If the network request fails
     * @throws JiraApiException If the Jira API returns an error
     */
    override fun addIssueFixVersion(
        issue: String,
        version: String,
    ) {
        val request =
            AddFixVersionRequest(
                update =
                    AddFixVersionRequest.Update(
                        fixVersions =
                            listOf(
                                AddFixVersionRequest.FixVersion(
                                    AddFixVersionRequest.FixVersion.Description(
                                        name = version,
                                    ),
                                ),
                            ),
                    ),
            )
        api
            .addFixVersion(issue, request)
            .executeNoResult()
            .onFailure { logger.error("Failed to add fix version for $issue", it) }
    }

    /**
     * Removes a fix version from a Jira issue.
     *
     * @param issue The issue key (e.g., "PROJ-123")
     * @param version The version to remove
     */
    override fun removeIssueFixVersion(
        issue: String,
        version: String,
    ) {
        val request =
            RemoveFixVersionRequest(
                update =
                    RemoveFixVersionRequest.Update(
                        fixVersions =
                            listOf(
                                RemoveFixVersionRequest.FixVersionRemove(
                                    remove = RemoveFixVersionRequest.VersionName(name = version),
                                ),
                            ),
                    ),
            )

        api.removeFixVersion(issue, request)
            .executeNoResult()
    }

    /**
     * Gets fix versions assigned to an issue.
     *
     * @param issue The issue key (e.g., "PROJ-123")
     */
    override fun getIssueFixVersions(issue: String): List<JiraFixVersion> {
        return api.getFixVersions(issue)
            .executeWithResult()
            .getOrNull()
            ?.fields
            ?.fixVersions
            .orEmpty()
            .map {
                JiraFixVersion(
                    id = it.id,
                    name = it.name,
                )
            }
    }

}
