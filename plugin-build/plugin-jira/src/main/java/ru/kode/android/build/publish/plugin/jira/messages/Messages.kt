package ru.kode.android.build.publish.plugin.jira.messages

fun pluginInitializedMessage(authNames: Set<String>, automationNames: Set<String>): String {
    return """
        |
        |============================================================
        |           🚀 JIRA PLUGIN INITIALIZED SUCCESSFULLY 🚀
        |============================================================
        | Jira plugin has been initialized with the following configurations:
        |
        | Authentication Configurations: ${if (authNames.isEmpty()) "None" else authNames.joinToString(", ")}
        | Automation Configurations: ${if (automationNames.isEmpty()) "None" else automationNames.joinToString(", ")}
        |
        | NEXT STEPS:
        | 1. Verify the configurations above match your Jira setup
        | 2. Check the logs for any warnings or errors
        | 3. Run your build to test the integration
        |============================================================
    """.trimIndent()
}

fun jiraServicesCreatedMessage(servicesNames: Set<String>): String {
    return """
        |
        |============================================================
        |           🔧 JIRA SERVICES CREATED SUCCESSFULLY 🔧
        |============================================================
        | The following Jira services have been created and configured:
        |
        | ${servicesNames.joinToString("\n") { "• $it" }}
        |
        | These services will be used for interacting with Jira API
        | and managing issue tracking during the build process.
        |============================================================
    """.trimIndent()
}

fun noAuthConfigsMessage(): String {
    return """
        |
        |============================================================
        |             ℹ️ NO JIRA AUTH CONFIGURATIONS FOUND ℹ️
        |============================================================
        | No Jira authentication configurations were found in your build script.
        | The service map will remain empty and Jira integration will be disabled.
        |
        | TO ENABLE JIRA INTEGRATION:
        | 1. Add a 'jira' configuration block to your build script
        | 2. Configure at least one authentication method
        |
        | Example configuration:
        | jira {
        |     auth {
        |         common {
        |             baseUrl = "https://your-domain.atlassian.net"
        |             username = providers.gradleProperty("jira.username")
        |             password = providers.gradleProperty("jira.apiToken")
        |         }
        |     }
        | }
        |
        | NOTE: Store sensitive credentials in gradle.properties or environment variables.
        |============================================================
    """.trimIndent()
}

fun mustApplyFoundationPluginMessage(): String {
    return """
        |
        |============================================================
        |              🚨 PLUGIN CONFIGURATION ERROR 🚨
        |============================================================
        | The Jira plugin requires the BuildPublishFoundationPlugin
        | to be applied first.
        |
        | REQUIRED ACTION:
        | Add the following to your module's build.gradle.kts file:
        |
        | plugins {
        |     id("ru.kode.android.build-publish-novo.foundation") version "<version>"
        | }
        |
        | Make sure to replace <version> with the correct version number.
        |============================================================
    """.trimIndent()
}

fun serviceExtensionCreatedMessage(): String {
    return """
        |
        |============================================================
        |             ℹ️ JIRA EXTENSION INITIALIZED ℹ️
        |============================================================
        | JiraServiceExtension has been created but no configuration
        | has been provided yet.
        |
        | NEXT STEPS:
        | 1. Configure your Jira settings in the build script
        | 2. Set up authentication and automation blocks
        | 3. Apply the configuration using the 'jira' extension
        |
        | Example configuration:
        | jira {
        |     auth { ... }
        |     automation { ... }
        | }
        |============================================================
    """.trimIndent()
}

fun issuesNoFoundMessage(): String {
    return """
        |
        |============================================================
        |             ℹ️ NO ISSUES FOUND IN CHANGELOG ℹ️
        |============================================================
        | No Jira issue keys were found in the changelog.
        | No Jira automation will be performed for this build.
        |
        | This is not an error, just an informational message.
        | If you expected to see Jira issues, please check:
        | 1. Your commit messages include Jira issue keys (e.g., PROJ-123)
        | 2. The changelog format includes the issue references
        | 3. The Jira project key matches your commit messages
        |============================================================
    """.trimIndent()
}

fun failedToUpdateStatusMessage(issue: String?): String {
    return """
        |
        |============================================================
        |          ⚠️ FAILED TO UPDATE ISSUE STATUS ⚠️
        |============================================================
        | Could not update status for issue: ${issue ?: "Unknown"}
        |
        | POSSIBLE CAUSES:
        | 1. Insufficient permissions to transition the issue
        | 2. Invalid or non-existent issue key
        | 3. Network connectivity issues with Jira
        | 4. Invalid transition configuration
        |
        | RECOMMENDED ACTIONS:
        | 1. Verify the issue key is correct
        | 2. Check your Jira permissions for this issue
        | 3. Review the full error in the logs
        | 4. Try updating the status manually in Jira to test permissions
        |============================================================
    """.trimIndent()
}

fun needToProvideAutomationConfigMessage(variantName: String): String {
    return """
        |
        |============================================================
        |           ⚠️ MISSING AUTOMATION CONFIGURATION ⚠️
        |============================================================
        | No Jira automation configuration found for build variant: $variantName
        |
        | REQUIRED ACTION:
        | You must provide an automation configuration in your build script.
        |
        | EXAMPLE CONFIGURATION:
        | jira {
        |     // For variant-specific configuration
        |     $variantName {
        |         automation {
        |             addFixVersion = true
        |             versionName = project.version.toString()
        |             // Other automation settings...
        |         }
        |     }
        |     
        |     // Or for common configuration
        |     common {
        |         automation {
        |             // Common automation settings...
        |         }
        |     }
        | }
        |
        | For more details, check the plugin documentation.
        |============================================================
    """.trimIndent()
}

fun needToProvideAuthConfigMessage(variantName: String): String {
    return """
        |
        |============================================================
        |             ⚠️ MISSING AUTHENTICATION ⚠️
        |============================================================
        | No Jira authentication configuration found for build variant: $variantName
        |
        | REQUIRED ACTION:
        | You must provide authentication details to use Jira integration.
        |
        | EXAMPLE CONFIGURATION:
        | jira {
        |     // For variant-specific authentication
        |     $variantName {
        |         auth {
        |             baseUrl = "https://your-domain.atlassian.net"
        |             username = providers.gradleProperty("jira.username")
        |             password = providers.gradleProperty("jira.apiToken")
        |         }
        |     }
        |     
        |     // Or for common authentication
        |     common {
        |         auth {
        |             // Common authentication settings...
        |         }
        |     }
        | }
        |
        | SECURITY NOTE:
        | • Never commit credentials directly in build files
        | • Use gradle.properties or environment variables for sensitive data
        | • Consider using API tokens instead of passwords
        |============================================================
    """.trimIndent()
}

fun failedToGetIssueFixVersionMessage(issue: String): String {
    return """
        |
        |============================================================
        |        ⚠️ FAILED TO RETRIEVE FIX VERSIONS ⚠️
        |============================================================
        | Could not retrieve fix versions for issue: $issue
        |
        | POSSIBLE CAUSES:
        | 1. Insufficient permissions to view the issue
        | 2. The issue might have been deleted or moved
        | 3. Network connectivity issues with Jira
        |
        | TROUBLESHOOTING:
        | 1. Verify the issue key is correct
        | 2. Check your Jira permissions for this issue
        | 3. Try accessing the issue in the Jira web interface
        | 4. Review the full error in the logs
        |============================================================
    """.trimIndent()
}

fun failedToRemoveFixVersionMessage(issue: String): String {
    return """
        |
        |============================================================
        |        ⚠️ FAILED TO REMOVE FIX VERSION ⚠️
        |============================================================
        | Could not remove fix version from issue: $issue
        |
        | POSSIBLE CAUSES:
        | 1. Insufficient permissions to modify the issue
        | 2. The fix version might be required by a workflow
        | 3. The issue might be in a read-only state
        |
        | RECOMMENDED ACTIONS:
        | 1. Verify your Jira permissions
        | 2. Check the issue's current status and workflow
        | 3. Try removing the fix version manually in Jira
        | 4. Review the full error in the logs
        |============================================================
    """.trimIndent()
}

fun failedToAddFixVersionMessage(issue: String, version: String): String {
    return """
        
        |============================================================
        |         ⚠️ FAILED TO ADD FIX VERSION ⚠️
        |============================================================
        | Could not add $version fix version to issue: $issue
        |
        | POSSIBLE CAUSES:
        | 1. The version might not exist in the project
        | 2. Insufficient permissions to modify the issue
        | 3. The version might be invalid for the issue's project
        |
        | TROUBLESHOOTING:
        | 1. Verify the version exists in the project
        | 2. Check your Jira permissions
        | 3. Try adding the version manually in Jira
        | 4. Review the full error in the logs
        |============================================================
    """.trimIndent().trim()
}

fun failedToRemoveVersionMessage(versionId: String): String {
    return """
        |
        |============================================================
        |           ⚠️ FAILED TO REMOVE VERSION ⚠️
        |============================================================
        | Could not remove version: $versionId
        |
        | POSSIBLE CAUSES:
        | 1. The version might be in use by issues
        | 2. Insufficient permissions to manage versions
        | 3. The version might be part of a release
        |
        | RECOMMENDED ACTIONS:
        | 1. Check if the version is used by any issues
        | 2. Verify your Jira project admin permissions
        | 3. Try archiving the version instead of deleting
        | 4. Review the full error in the logs
        |============================================================
    """.trimIndent()
}

fun failedToCreateProjectVersionMessage(version: String, projectId: Long): String {
    return """
        |
        |============================================================
        |         ⚠️ FAILED TO CREATE PROJECT VERSION ⚠️
        |============================================================
        | Could not create version: $version
        | Project ID: $projectId
        |
        | POSSIBLE CAUSES:
        | 1. A version with this name already exists
        | 2. Invalid version format or name
        | 3. Insufficient permissions to create versions
        | 4. The project might be archived or deleted
        |
        | TROUBLESHOOTING:
        | 1. Check if the version already exists
        | 2. Verify the version name follows Jira's requirements
        | 3. Check your Jira project admin permissions
        | 4. Review the full error in the logs
        |============================================================
    """.trimIndent()
}

fun failedToRemoveMessage(issue: String): String {
    return """
        |
        |============================================================
        |          ⚠️ FAILED TO REMOVE LABEL ⚠️
        |============================================================
        | Could not remove label from issue: $issue
        |
        | POSSIBLE CAUSES:
        | 1. The label might not exist on the issue
        | 2. Insufficient permissions to modify the issue
        | 3. The issue might be in a read-only state
        |
        | RECOMMENDED ACTIONS:
        | 1. Verify the issue exists and is accessible
        | 2. Check your Jira permissions for this issue
        | 3. Try removing the label manually in Jira
        | 4. Review the full error in the logs
        |============================================================
    """.trimIndent()
}

fun failedToAddLabelMessage(issue: String, label: String): String {
    return """
        
        |============================================================
        |           ⚠️ FAILED TO ADD LABEL ⚠️
        |============================================================
        | Could not add ${label} label to issue: $issue
        |
        | POSSIBLE CAUSES:
        | 1. The label might be too long (max 255 chars)
        | 2. The label contains invalid characters
        | 3. Insufficient permissions to modify the issue
        | 4. The issue might be in a read-only state
        |
        | TROUBLESHOOTING:
        | 1. Verify the label follows Jira's naming conventions
        | 2. Check your Jira permissions for this issue
        | 3. Try adding the label manually in Jira
        | 4. Review the full error in the logs
        |============================================================
    """.trimIndent().trim()
}

fun failedToGetIssueStatusMessage(issue: String): String {
    return """
        |
        |============================================================
        |        ⚠️ FAILED TO GET ISSUE STATUS ⚠️
        |============================================================
        | Could not retrieve status for issue: $issue
        |
        | POSSIBLE CAUSES:
        | 1. The issue might have been deleted or moved
        | 2. Insufficient permissions to view the issue
        | 3. Network connectivity issues with Jira
        |
        | RECOMMENDED ACTIONS:
        | 1. Verify the issue key is correct
        | 2. Check your Jira permissions for this issue
        | 3. Try accessing the issue in the Jira web interface
        | 4. Review the full error in the logs
        |============================================================
    """.trimIndent()
}

fun failedToSetStatusMessage(issue: String): String {
    return """
        |
        |============================================================
        |         ⚠️ FAILED TO SET ISSUE STATUS ⚠️
        |============================================================
        | Could not update status for issue: $issue
        |
        | POSSIBLE CAUSES:
        | 1. The target status is not a valid transition
        | 2. Missing required fields for the transition
        | 3. Insufficient permissions to transition the issue
        | 4. The issue might be in a read-only state
        |
        | TROUBLESHOOTING:
        | 1. Check the available transitions for this issue
        | 2. Verify all required fields are set
        | 3. Check your Jira permissions
        | 4. Try the transition manually in Jira
        | 5. Review the full error in the logs
        |============================================================
    """.trimIndent()
}

fun issueTransitionNotFoundMessage(issueKeyWithTransitions: String, statusName: String): String {
    return """
        |
        |============================================================
        |          ⚠️ INVALID STATUS TRANSITION ⚠️
        |============================================================
        | Issue: $issueKeyWithTransitions
        | Target Status: $statusName
        |
        | The specified status transition is not available for this issue.
        |
        | POSSIBLE CAUSES:
        | 1. The workflow doesn't allow this transition from the current status
        | 2. The target status doesn't exist in this project
        | 3. You don't have permission to perform this transition
        |
        | REQUIRED ACTION:
        | 1. Check the current status of the issue in Jira
        | 2. Review the available transitions in the issue workflow
        | 3. Add the missing transition in Jira workflow settings if needed
        | 4. Ensure you have the necessary permissions
        |
        | You can perform this transition manually in the Jira web interface
        | to see the exact requirements and available options.
        |============================================================
    """.trimIndent()
}

fun issueStatusNotFoundMessage(statusName: String): String {
    return """
        |
        |============================================================
        |            ⚠️ STATUS NOT FOUND ⚠️
        |============================================================
        | The specified status was not found: $statusName
        |
        | POSSIBLE CAUSES:
        | 1. The status name is misspelled
        | 2. The status doesn't exist in the project's workflow
        | 3. The status exists but is not available in the current context
        |
        | RECOMMENDED ACTIONS:
        | 1. Verify the status name matches exactly (case-sensitive)
        | 2. Check the available statuses in your Jira project
        | 3. Review the workflow configuration in Jira
        | 4. Consult your Jira administrator if needed
        |
        | TIP: You can view all available statuses in your Jira project
        | by checking the workflow configuration.
        |============================================================
    """.trimIndent()
}

fun statusNotFoundMessage(statusName: String, projectKey: String): String {
    return """
        |
        |============================================================
        |       ⚠️ STATUS NOT FOUND IN PROJECT ⚠️
        |============================================================
        | Status: $statusName
        | Project: $projectKey
        |
        | The specified status was not found in the project's workflow.
        |
        | POSSIBLE CAUSES:
        | 1. The status name is misspelled
        | 2. The status hasn't been added to this project's workflow
        | 3. The status exists but is not active
        |
        | REQUIRED ACTION:
        | 1. Verify the status name is correct
        | 2. Add the status to your project's workflow in Jira:
        |    a. Go to Project Settings > Workflows
        |    b. Edit the workflow
        |    c. Add the status and configure transitions
        |    d. Publish the workflow
        |
        | NOTE: You need Jira administrator permissions to modify workflows.
        |============================================================
    """.trimIndent()
}
