package ru.kode.android.build.publish.plugin.jira.controller

import org.gradle.api.GradleException
import ru.kode.android.build.publish.plugin.core.logger.PluginLogger
import ru.kode.android.build.publish.plugin.core.util.executeNoResult
import ru.kode.android.build.publish.plugin.core.util.executeWithResult
import ru.kode.android.build.publish.plugin.jira.controller.entity.JiraFixVersion
import ru.kode.android.build.publish.plugin.jira.controller.entity.JiraIssueStatus
import ru.kode.android.build.publish.plugin.jira.controller.entity.JiraIssueTransition
import ru.kode.android.build.publish.plugin.jira.messages.failedToAddFixVersionMessage
import ru.kode.android.build.publish.plugin.jira.messages.failedToAddLabelMessage
import ru.kode.android.build.publish.plugin.jira.messages.failedToCreateProjectVersionMessage
import ru.kode.android.build.publish.plugin.jira.messages.failedToGetIssueFixVersionMessage
import ru.kode.android.build.publish.plugin.jira.messages.failedToGetIssueStatusMessage
import ru.kode.android.build.publish.plugin.jira.messages.failedToRemoveFixVersionMessage
import ru.kode.android.build.publish.plugin.jira.messages.failedToRemoveMessage
import ru.kode.android.build.publish.plugin.jira.messages.failedToRemoveVersionMessage
import ru.kode.android.build.publish.plugin.jira.messages.failedToSetStatusMessage
import ru.kode.android.build.publish.plugin.jira.messages.issueStatusNotFoundMessage
import ru.kode.android.build.publish.plugin.jira.messages.issueTransitionNotFoundMessage
import ru.kode.android.build.publish.plugin.jira.messages.statusNotFoundMessage
import ru.kode.android.build.publish.plugin.jira.messages.transitionIdResolved
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
    private val logger: PluginLogger,
) : JiraController {
    /**
     * Retrieves the ID of the status with the given name in the specified project,
     * using the first issue in the list that has a transition to this status.
     *
     * @param projectKey The key of the Jira project
     * @param statusName The name of the status to search for
     * @param issues A list of issue keys to search for a transition to the given status
     * @return The ID of the status with the given name
     *
     * @throws IOException If the network request fails
     * @throws JiraApiException If the Jira API returns an error
     */
    override fun getStatusTransitionId(
        projectKey: String,
        statusName: String,
        issues: List<String>,
    ): String? {
        val statuses = getProjectAvailableStatuses(projectKey)
        val status =
            statuses
                .firstOrNull { it.name.equals(statusName, ignoreCase = true) }
                ?: throw GradleException(statusNotFoundMessage(statusName, projectKey))

        val statusId = status.id
        val issueKeyWithTransitions =
            issues
                .firstOrNull { issueKey ->
                    getAvailableIssueTransitions(issueKey)
                        .find { it.statusId == statusId } != null
                }
                ?: return null.also {
                    logger.quiet(issueStatusNotFoundMessage(statusName))
                }

        val transition =
            getAvailableIssueTransitions(issueKeyWithTransitions)
                .firstOrNull { it.statusId == statusId }
                ?: return null.also {
                    logger.quiet(issueTransitionNotFoundMessage(issueKeyWithTransitions, statusName))
                }

        return transition
            .id
            .also { id -> logger.info(transitionIdResolved(id, statusName)) }
    }

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
        api
            .setStatus(issue, request)
            .executeNoResult()
            .onFailure { logger.error(failedToSetStatusMessage(issue), it) }
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
        val status =
            api.getStatus(issue)
                .executeWithResult()
                .onFailure { logger.error(failedToGetIssueStatusMessage(issue), it) }
                .getOrNull()
                ?.fields
                ?.status

        return status?.let {
            JiraIssueStatus(
                id = it.id,
                name = it.name,
            )
        }
    }

    /**
     * Retrieves all statuses available in a Jira project across all workflows.
     *
     * Calls:
     * `GET /rest/api/2/project/{projectKey}/statuses`
     *
     * Example returned values:
     * - ID: "1", Name: "Open"
     * - ID: "3", Name: "In Progress"
     * - ID: "5", Name: "Resolved"
     */
    override fun getProjectAvailableStatuses(projectKey: String): List<JiraIssueStatus> {
        val workflows =
            api.getProjectStatuses(projectKey)
                .executeWithResult()
                .getOrThrow()

        return workflows
            .flatMap { it.statuses }
            .map { status ->
                JiraIssueStatus(
                    id = status.id,
                    name = status.name,
                )
            }
            .distinctBy { it.id }
    }

    /**
     * Retrieves the ID of a Jira project by its key.
     *
     * @param projectKey The key of the project (e.g., "PROJECT")
     *
     * @return The ID of the project
     *
     * @throws IOException If the network request fails
     * @throws JiraApiException If the Jira API returns an error
     */
    override fun getProjectId(projectKey: String): Long {
        val project =
            api.getProject(projectKey)
                .executeWithResult()
                .getOrThrow()

        return project.id
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
            .onFailure { logger.error(failedToAddLabelMessage(issue, label), it) }
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
            .onFailure { logger.error(failedToRemoveMessage(issue), it) }
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
        api.createVersion(request)
            .executeNoResult()
            .onFailure { logger.error(failedToCreateProjectVersionMessage(version, projectId), it) }
    }

    /**
     * Removes a version from a Jira project.
     *
     * @param versionId ID of the version to delete
     *
     * @throws IOException If request fails
     * @throws JiraApiException If Jira API returns an error
     */
    override fun removeProjectVersion(versionId: String) {
        api
            .deleteVersion(
                versionId = versionId,
                moveFixIssuesTo = null,
                moveAffectedIssuesTo = null,
            )
            .executeNoResult()
            .onFailure { logger.error(failedToRemoveVersionMessage(versionId), it) }
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
                    name = it.name,
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
            .onFailure { logger.error(failedToAddFixVersionMessage(issue, version), it) }
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
            .onFailure { logger.error(failedToRemoveFixVersionMessage(issue), it) }
    }

    /**
     * Gets fix versions assigned to an issue.
     *
     * @param issue The issue key (e.g., "PROJ-123")
     */
    override fun getIssueFixVersions(issue: String): List<JiraFixVersion> {
        return api.getFixVersions(issue)
            .executeWithResult()
            .onFailure { logger.error(failedToGetIssueFixVersionMessage(issue), it) }
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

    /**
     * Retrieves all available transitions for a Jira issue.
     *
     * This method calls:
     * `GET /rest/api/2/issue/{issue}/transitions`
     *
     * Jira returns a list of transitions, where each transition contains:
     * - transition ID (not used in domain)
     * - transition name (not used in domain)
     * - the target status object (ID + name) → this is what we need
     *
     * Example returned transitions:
     * - ID: "1", Name: "Start Progress", TargetStatus: "In Progress"
     * - ID: "2", Name: "Resolve", TargetStatus: "Resolved"
     *
     * If no transitions are available, returns an empty list.
     *
     * @param issue The Jira issue key (e.g., "PROJ-123").
     *
     * @return A list of `JiraIssueTransition` domain objects.
     *
     * @throws IOException If HTTP request fails.
     * @throws JiraApiException If Jira responds with error code or unexpected result.
     */
    private fun getAvailableIssueTransitions(issue: String): List<JiraIssueTransition> {
        val response =
            api.getAvailableTransitions(issue)
                .executeWithResult()
                .getOrThrow()

        return response.transitions
            .map { transition ->
                JiraIssueTransition(
                    id = transition.id,
                    name = transition.name,
                    statusId = transition.to.id,
                )
            }
    }
}
