package ru.kode.android.build.publish.plugin.jira.controller

import ru.kode.android.build.publish.plugin.jira.controller.entity.JiraFixVersion
import ru.kode.android.build.publish.plugin.jira.controller.entity.JiraIssueStatus
import ru.kode.android.build.publish.plugin.jira.controller.entity.JiraIssueTransition

/**
 * Controller for interacting with the Jira API.
 */
interface JiraController {

    /**
     * Transitions a Jira issue to a new status.
     *
     * @param issue The issue key (e.g., "PROJ-123")
     * @param statusTransitionId The ID of the status transition to execute
     *
     * @throws IOException If the network request fails
     * @throws JiraApiException If the Jira API returns an error
     */
    fun setIssueStatus(issue: String, statusTransitionId: String)

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
    fun getIssueStatus(issue: String): JiraIssueStatus?

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
    fun getAvailableIssueTransitions(issue: String): List<JiraIssueTransition>

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
    fun getProjectAvailableStatuses(projectKey: String): List<JiraIssueStatus>

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
    fun getProjectId(projectKey: String): Long

    /**
     * Adds a label to a Jira issue.
     *
     * @param issue The issue key (e.g., "PROJ-123")
     * @param label The label to add
     *
     * @throws IOException If the network request fails
     * @throws JiraApiException If the Jira API returns an error
     */
    fun addIssueLabel(issue: String, label: String)

    /**
     * Removes a label from a Jira issue.
     *
     * @param issue The issue key (e.g., "PROJ-123")
     * @param label The label to remove
     *
     * @throws IOException If the network request fails
     * @throws JiraApiException If the Jira API returns an error
     */
    fun removeIssueLabel(issue: String, label: String)

    /**
     * Retrieves labels of a Jira issue.
     *
     * @param issue The issue key (e.g. "PROJ-123")
     * @return List of labels attached to the issue
     *
     * @throws IOException If the network request fails
     * @throws JiraApiException If the Jira API returns an error
     */
    fun getIssueLabels(issue: String): List<String>

    /**
     * Creates a new version in a Jira project.
     *
     * @param projectId The ID of the Jira project
     * @param version The version name to create
     *
     * @throws IOException If the network request fails
     * @throws JiraApiException If the Jira API returns an error or version already exists
     */
    fun createProjectVersion(projectId: Long, version: String)

    /**
     * Removes a version from a Jira project.
     *
     * @param versionId ID of the version to delete
     *
     * @throws IOException If request fails
     * @throws JiraApiException If Jira API returns an error
     */
    fun removeProjectVersion(versionId: String)

    /**
     * Retrieves all versions of a Jira project.
     *
     * @param projectKey The key of the Jira project
     * @return List of project versions
     *
     * @throws IOException If the network request fails
     * @throws JiraApiException If the Jira API returns an error
     */
    fun getProjectVersions(projectKey: String): List<JiraFixVersion>

    /**
     * Adds a fix version to a Jira issue.
     *
     * @param issue The issue key (e.g., "PROJ-123")
     * @param version The version to add as a fix version
     *
     * @throws IOException If the network request fails
     * @throws JiraApiException If the Jira API returns an error
     */
    fun addIssueFixVersion(issue: String, version: String)

    /**
     * Removes a fix version from a Jira issue.
     *
     * @param issue The issue key (e.g., "PROJ-123")
     * @param version The version to remove
     */
    fun removeIssueFixVersion(issue: String, version: String)

    /**
     * Gets fix versions assigned to an issue.
     *
     * @param issue The issue key (e.g., "PROJ-123")
     */
    fun getIssueFixVersions(issue: String): List<JiraFixVersion>

}
