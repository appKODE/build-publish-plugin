package ru.kode.android.build.publish.plugin.jira.controller

import ru.kode.android.build.publish.plugin.jira.controller.entity.JiraFixVersion
import ru.kode.android.build.publish.plugin.jira.controller.entity.JiraIssueStatus

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
    fun removeProjectVersion(versionId: Long)

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
