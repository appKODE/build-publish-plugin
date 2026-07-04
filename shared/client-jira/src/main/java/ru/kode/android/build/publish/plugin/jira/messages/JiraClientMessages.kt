package ru.kode.android.build.publish.plugin.jira.messages

fun addingLabelToIssuesMessage(
    label: String,
    count: Int,
): String = "Adding label '$label' to $count Jira issues"

fun addingFixVersionToIssuesMessage(
    version: String,
    count: Int,
): String = "Adding fix version '$version' to $count Jira issues"

fun transitioningIssuesMessage(
    count: Int,
    transitionName: String,
): String = "Transitioning $count Jira issues to '$transitionName'"

fun transitionNotFoundMessage(
    transitionName: String,
    projectKey: String,
): String = "No transition '$transitionName' found for project '$projectKey'"

fun failedToAddFixVersionMessage(
    issue: String,
    version: String,
): String {
    return """

        |============================================================
        |                 FAILED TO ADD FIX VERSION
        |============================================================
        | Could not add $version fix version to issue: $issue
        |
        | POSSIBLE CAUSES:
        |  1. The version might not exist in the project
        |  2. Insufficient permissions to modify the issue
        |  3. The version might be invalid for the issue's project
        |
        | TROUBLESHOOTING:
        |  1. Verify the version exists in the project
        |  2. Check your Jira permissions
        |  3. Try adding the version manually in Jira
        |  4. Review the full error in the logs
        |============================================================
        """.trimIndent().trim()
}

fun failedToAddLabelMessage(
    issue: String,
    label: String,
): String {
    return """

        |============================================================
        |                   FAILED TO ADD LABEL
        |============================================================
        | Could not add $label label to issue: $issue
        |
        | POSSIBLE CAUSES:
        |  1. The label might be too long (max 255 chars)
        |  2. The label contains invalid characters
        |  3. Insufficient permissions to modify the issue
        |  4. The issue might be in a read-only state
        |
        | TROUBLESHOOTING:
        |  1. Verify the label follows Jira's naming conventions
        |  2. Check your Jira permissions for this issue
        |  3. Try adding the label manually in Jira
        |  4. Review the full error in the logs
        |============================================================
        """.trimIndent().trim()
}

fun failedToCreateProjectVersionMessage(
    version: String,
    projectId: Long,
): String {
    return """

        |============================================================
        |             FAILED TO CREATE PROJECT VERSION
        |============================================================
        | Could not create version: $version
        | Project ID: $projectId
        |
        | POSSIBLE CAUSES:
        |  1. A version with this name already exists
        |  2. Invalid version format or name
        |  3. Insufficient permissions to create versions
        |  4. The project might be archived or deleted
        |
        | TROUBLESHOOTING:
        |  1. Check if the version already exists
        |  2. Verify the version name follows Jira's requirements
        |  3. Check your Jira project admin permissions
        |  4. Review the full error in the logs
        |============================================================
        """.trimIndent()
}

fun failedToGetIssueFixVersionMessage(issue: String): String {
    return """

        |============================================================
        |              FAILED TO RETRIEVE FIX VERSIONS
        |============================================================
        | Could not retrieve fix versions for issue: $issue
        |
        | POSSIBLE CAUSES:
        |  1. Insufficient permissions to view the issue
        |  2. The issue might have been deleted or moved
        |  3. Network connectivity issues with Jira
        |
        | TROUBLESHOOTING:
        |  1. Verify the issue key is correct
        |  2. Check your Jira permissions for this issue
        |  3. Try accessing the issue in the Jira web interface
        |  4. Review the full error in the logs
        |============================================================
        """.trimIndent()
}

fun failedToGetIssueStatusMessage(issue: String): String {
    return """

        |============================================================
        |                FAILED TO GET ISSUE STATUS
        |============================================================
        | Could not retrieve status for issue: $issue
        |
        | POSSIBLE CAUSES:
        |  1. The issue might have been deleted or moved
        |  2. Insufficient permissions to view the issue
        |  3. Network connectivity issues with Jira
        |
        | RECOMMENDED ACTIONS:
        |  1. Verify the issue key is correct
        |  2. Check your Jira permissions for this issue
        |  3. Try accessing the issue in the Jira web interface
        |  4. Review the full error in the logs
        |============================================================
        """.trimIndent()
}

fun failedToGetIssueSummaryMessage(issue: String): String = "Could not retrieve summary (title) for Jira issue: $issue"

fun failedToRemoveFixVersionMessage(issue: String): String {
    return """

        |============================================================
        |               FAILED TO REMOVE FIX VERSION
        |============================================================
        | Could not remove fix version from issue: $issue
        |
        | POSSIBLE CAUSES:
        |  1. Insufficient permissions to modify the issue
        |  2. The fix version might be required by a workflow
        |  3. The issue might be in a read-only state
        |
        | RECOMMENDED ACTIONS:
        |  1. Verify your Jira permissions
        |  2. Check the issue's current status and workflow
        |  3. Try removing the fix version manually in Jira
        |  4. Review the full error in the logs
        |============================================================
        """.trimIndent()
}

fun failedToRemoveMessage(issue: String): String {
    return """

        |============================================================
        |                  FAILED TO REMOVE LABEL
        |============================================================
        | Could not remove label from issue: $issue
        |
        | POSSIBLE CAUSES:
        |  1. The label might not exist on the issue
        |  2. Insufficient permissions to modify the issue
        |  3. The issue might be in a read-only state
        |
        | RECOMMENDED ACTIONS:
        |  1. Verify the issue exists and is accessible
        |  2. Check your Jira permissions for this issue
        |  3. Try removing the label manually in Jira
        |  4. Review the full error in the logs
        |============================================================
        """.trimIndent()
}

fun failedToRemoveVersionMessage(versionId: String): String {
    return """

        |============================================================
        |                 FAILED TO REMOVE VERSION
        |============================================================
        | Could not remove version: $versionId
        |
        | POSSIBLE CAUSES:
        |  1. The version might be in use by issues
        |  2. Insufficient permissions to manage versions
        |  3. The version might be part of a release
        |
        | RECOMMENDED ACTIONS:
        |  1. Check if the version is used by any issues
        |  2. Verify your Jira project admin permissions
        |  3. Try archiving the version instead of deleting
        |  4. Review the full error in the logs
        |============================================================
        """.trimIndent()
}

fun failedToSetStatusMessage(issue: String): String {
    return """

        |============================================================
        |               FAILED TO SET ISSUE STATUS
        |============================================================
        | Could not update status for issue: $issue
        |
        | POSSIBLE CAUSES:
        |  1. The target status is not a valid transition
        |  2. Missing required fields for the transition
        |  3. Insufficient permissions to transition the issue
        |  4. The issue might be in a read-only state
        |
        | TROUBLESHOOTING:
        |  1. Check the available transitions for this issue
        |  2. Verify all required fields are set
        |  3. Check your Jira permissions
        |  4. Try the transition manually in Jira
        |  5. Review the full error in the logs
        |============================================================
        """.trimIndent()
}

fun issueStatusNotFoundMessage(statusName: String): String {
    return """

        |============================================================
        |                      STATUS NOT FOUND
        |============================================================
        | The specified status was not found: $statusName
        |
        | POSSIBLE CAUSES:
        |  1. The status name is misspelled
        |  2. The status doesn't exist in the project's workflow
        |  3. The status exists but is not available in the current
        |    context
        |
        | RECOMMENDED ACTIONS:
        |  1. Verify the status name matches exactly (case-sensitive)
        |  2. Check the available statuses in your Jira project
        |  3. Review the workflow configuration in Jira
        |  4. Consult your Jira administrator if needed
        |
        | TIP:
        | You can view all available statuses in your Jira
        | project by checking the workflow configuration.
        |============================================================
        """.trimIndent()
}

fun issueTransitionNotFoundMessage(
    issueKeyWithTransitions: String,
    statusName: String,
): String {
    return """

        |============================================================
        |                 INVALID STATUS TRANSITION
        |============================================================
        | Issue: $issueKeyWithTransitions
        | Target Status: $statusName
        |
        | The specified status transition is not available for this
        | issue.
        |
        | POSSIBLE CAUSES:
        | 1. The workflow doesn't allow this transition from the
        |    current status
        | 2. The target status doesn't exist in this project
        | 3. You don't have permission to perform this transition
        |
        | REQUIRED ACTION:
        | 1. Check the current status of the issue in Jira
        | 2. Review the available transitions in the issue workflow
        | 3. Add the missing transition in Jira workflow settings
        |    if needed
        | 4. Ensure you have the necessary permissions
        |
        | You can perform this transition manually in the Jira web
        | interface to see the exact requirements and available
        | options.
        |============================================================
        """.trimIndent()
}

fun statusNotFoundMessage(
    statusName: String,
    projectKey: String,
): String {
    return """

        |============================================================
        |               STATUS NOT FOUND IN PROJECT
        |============================================================
        | Status: $statusName
        | Project: $projectKey
        |
        | The specified status was not found in the project's
        | workflow.
        |
        | POSSIBLE CAUSES:
        |  1. The status name is misspelled
        |  2. The status hasn't been added to this project's workflow
        |  3. The status exists but is not active
        |
        | REQUIRED ACTION:
        |  1. Verify the status name is correct
        |  2. Add the status to your project's workflow in Jira:
        |     a. Go to Project Settings > Workflows
        |     b. Edit the workflow
        |     c. Add the status and configure transitions
        |     d. Publish the workflow
        |
        | NOTE:
        | You need Jira administrator permissions to modify workflows.
        |============================================================
        """.trimIndent()
}

fun transitionIdResolved(
    id: String,
    statusName: String,
): String {
    return """

        |============================================================
        |              STATUS TRANSITION RESOLVED
        |============================================================
        | Successfully resolved status transition
        |
        | Transition ID: $id
        | Target Status: $statusName
        |
        | The issue will be transitioned to the specified status.
        |============================================================
        """.trimIndent()
}
