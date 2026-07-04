package ru.kode.android.build.publish.plugin.jira.messages

import ru.kode.android.build.publish.plugin.jira.EXTENSION_NAME

fun pluginInitializedMessage(
    instanceNames: Set<String>,
    automationNames: Set<String>,
): String {
    return """

        |============================================================
        |            JIRA PLUGIN INITIALIZED SUCCESSFULLY
        |============================================================
        | Jira plugin has been initialized with the following
        | configurations:
        |
        | Auth Instances:
        | ${if (instanceNames.isEmpty()) "None" else instanceNames.joinToString(", ")}
        | Automation Configurations:
        | ${if (automationNames.isEmpty()) "None" else automationNames.joinToString(", ")}
        |
        | NEXT STEPS:
        |  1. Verify the configurations above match your Jira setup
        |  2. Check the logs for any warnings or errors
        |  3. Run your build to test the integration
        |============================================================
        """.trimIndent()
}

fun registeringServicesMessage(): String {
    return """

        |============================================================
        |                 REGISTERING JIRA SERVICES
        |============================================================
        |  Initializing Jira service registration...
        |============================================================
        |
        """.trimIndent()
}

fun jiraServicesCreatedMessage(servicesNames: Set<String>): String {
    return """

        |============================================================
        |              JIRA SERVICES CREATED SUCCESSFULLY
        |============================================================
        | The following Jira services have been created and configured:
        |
        | ${servicesNames.joinToString(", ")}
        |
        | These services will be used for interacting with Jira API
        | and managing issue tracking during the build process.
        |============================================================
        """.trimIndent()
}

fun noAuthConfigsMessage(): String {
    return """

        |============================================================
        |              NO JIRA AUTH CONFIGURATIONS FOUND
        |============================================================
        | No Jira authentication configurations were found in your
        | build script.
        | The service map will remain empty and Jira integration will
        | be disabled.
        |
        | TO ENABLE JIRA INTEGRATION:
        |  1. Add a 'jira' configuration block to your build script
        |  2. Configure at least one authentication method
        |
        | Example configuration:
        |
        | $EXTENSION_NAME {
        |     auth {
        |         common {
        |             // Auth settings here
        |         }
        |     }
        | }
        |
        | NOTE:
        | Store sensitive credentials in gradle.properties or
        | environment variables.
        |============================================================
        """.trimIndent()
}

fun mustApplyFoundationPluginMessage(): String {
    return """

        |============================================================
        |                 PLUGIN CONFIGURATION ERROR
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
        | Make sure to replace <version> with the correct version
        | number.
        |============================================================
        """.trimIndent()
}

fun serviceExtensionCreatedMessage(): String {
    return """

        |============================================================
        |                 JIRA EXTENSION INITIALIZED
        |============================================================
        | JiraServiceExtension has been created but no configuration
        | has been provided yet.
        |
        | NEXT STEPS:
        |  1. Configure your Jira settings in the build script
        |  2. Set up authentication and automation blocks
        |  3. Apply the configuration using the 'jira' extension
        |
        | Example configuration:
        |
        |  $EXTENSION_NAME {
        |      auth { ... }
        |      automation { ... }
        |  }
        |============================================================
        """.trimIndent()
}

fun issuesNoFoundMessage(): String {
    return """

        |============================================================
        |                NO ISSUES FOUND IN CHANGELOG
        |============================================================
        | No Jira issue keys were found in the changelog.
        | No Jira automation will be performed for this build.
        |
        | If you expected to see Jira issues, please check:
        |  1. Your commit messages include Jira issue keys
        |     (e.g., PROJ-123)
        |  2. The changelog format includes the issue references
        |  3. The Jira project key matches your commit messages
        |============================================================
        """.trimIndent()
}

fun notPossibleToUpdateStatusMessage(issues: List<String>): String {
    return """

        |============================================================
        |            STATUS TRANSITION NOT AVAILABLE
        |============================================================
        | Could not update status for issues: ${issues.joinToString()}
        |
        | The status transition ID is null, which means no valid
        | transition was found for this issues.
        |
        | POSSIBLE CAUSES:
        |  1. The target status is not reachable from the current
        |     issue status
        |  2. The workflow doesn't define a transition to the
        |     target status
        |  3. None of the issues have the required transition
        |
        | RECOMMENDED ACTIONS:
        |  1. Check the current status of the issue in Jira
        |  2. Verify the target status name is correct
        |  3. Review the project workflow configuration
        |  4. Ensure at least one issue has the target transition
        |============================================================
        """.trimIndent()
}

fun failedToUpdateStatusMessage(issue: String): String {
    return """

        |============================================================
        |               FAILED TO UPDATE ISSUE STATUS
        |============================================================
        | Could not update status for issue: $issue
        |
        | POSSIBLE CAUSES:
        |  1. Insufficient permissions to transition the issue
        |  2. Invalid or non-existent issue key
        |  3. Network connectivity issues with Jira
        |  4. Invalid transition configuration
        |
        | RECOMMENDED ACTIONS:
        |  1. Verify the issue key is correct
        |  2. Check your Jira permissions for this issue
        |  3. Review the full error in the logs
        |  4. Try updating the status manually in Jira to test
        |     permissions
        |============================================================
        """.trimIndent()
}

fun needToProvideAutomationConfigMessage(variantName: String): String {
    return """

        |============================================================
        |              MISSING AUTOMATION CONFIGURATION
        |============================================================
        | No Jira automation configuration found for build variant:
        | $variantName
        |
        | REQUIRED ACTION:
        | You must provide an automation configuration in your
        | build script.
        |
        | EXAMPLE CONFIGURATION:
        |
        | $EXTENSION_NAME {
        |     common { // or buildVariant($variantName)
        |         automation {
        |             // Your automation settings here
        |         }
        |     }
        | }
        |
        | For more details, check the plugin documentation.
        |============================================================
        """.trimIndent()
}

fun unknownInstanceNameMessage(
    instanceName: String,
    availableInstanceNames: Collection<String>,
): String {
    return """

        |============================================================
        |                UNKNOWN JIRA AUTH INSTANCE
        |============================================================
        | A Jira project references the auth instance "$instanceName",
        | but no such instance exists.
        |
        | Available auth instances:
        | ${if (availableInstanceNames.isEmpty()) "None" else availableInstanceNames.sorted().joinToString(", ")}
        |
        | REQUIRED ACTION:
        | Declare the instance, or set 'instanceName' on the project to
        | one of the available names:
        |
        | $EXTENSION_NAME {
        |     auth {
        |         common {
        |             instance("$instanceName") {
        |                 baseUrl.set("https://...")
        |                 credentials.username.set("...")
        |                 credentials.password.set("...")
        |             }
        |         }
        |     }
        | }
        |============================================================
        """.trimIndent()
}

fun unknownProjectNameMessage(
    projectName: String,
    instanceName: String,
    availableProjectNames: Collection<String>,
): String {
    return """

        |============================================================
        |                UNKNOWN JIRA PROJECT
        |============================================================
        | A selection references the project "$projectName" on instance
        | "$instanceName", but no such project is declared on it.
        |
        | Available projects on "$instanceName":
        | ${if (availableProjectNames.isEmpty()) "None" else availableProjectNames.sorted().joinToString(", ")}
        |
        | REQUIRED ACTION:
        | Declare the project under the instance's registry, e.g.:
        |
        | $EXTENSION_NAME {
        |     auth { common { instance("$instanceName") {
        |         projects { project("$projectName") { projectKey.set("KEY") } }
        |     } } }
        | }
        |============================================================
        """.trimIndent()
}

fun duplicateProjectKeyMessage(projectKey: String): String {
    return """

        |============================================================
        |                DUPLICATE JIRA PROJECT KEY
        |============================================================
        | The project key "$projectKey" is declared by more than one
        | Jira project in the same automation configuration.
        |
        | Issues are routed to a project by their key prefix, so a
        | project key must be unique. Two projects with the same key
        | (even on different Jira instances) cannot be disambiguated.
        |
        | REQUIRED ACTION:
        | Ensure each configured project uses a distinct 'projectKey'.
        |============================================================
        """.trimIndent()
}

fun noIssuesForProjectMessage(projectKey: String): String {
    return """

        |============================================================
        |              NO CHANGELOG ISSUES FOR PROJECT
        |============================================================
        | The configured Jira project "$projectKey" did not match any
        | issue in the changelog, so nothing was updated for it.
        |
        | This is expected when the build simply has no "$projectKey"
        | issues. If you did expect some, check that the changelog
        | `issueSources` include a pattern covering "$projectKey"
        | (e.g. "$projectKey-\d+") and that commit messages reference it.
        |============================================================
        """.trimIndent()
}

fun unmatchedIssuesMessage(
    issues: Collection<String>,
    projectKeys: Collection<String>,
): String {
    return """

        |============================================================
        |              ISSUES NOT MATCHING ANY PROJECT
        |============================================================
        | The following issues were found in the changelog but do not
        | belong to any configured Jira project, so they were skipped:
        | ${issues.sorted().joinToString(", ")}
        |
        | Configured project keys:
        | ${if (projectKeys.isEmpty()) "None" else projectKeys.sorted().joinToString(", ")}
        |
        | If these issues should be processed, add a matching project
        | (with the correct 'projectKey') to your automation config.
        |============================================================
        """.trimIndent()
}

fun needToProvideAuthConfigMessage(variantName: String): String {
    return """

        |============================================================
        |                   MISSING AUTHENTICATION
        |============================================================
        | No Jira authentication configuration found for build variant:
        | $variantName
        |
        | REQUIRED ACTION:
        | You must provide authentication details to use Jira
        | integration.
        |
        | EXAMPLE CONFIGURATION:
        |
        | $EXTENSION_NAME {
        |     common { // or buildVariant($variantName)
        |         auth {
        |             // Your auth settings here
        |         }
        |     }
        | }
        |
        | SECURITY NOTE:
        | • Never commit credentials directly in build files
        | • Use local.properties or environment variables for
        |   sensitive data
        | • Consider using API tokens instead of passwords
        |============================================================
        """.trimIndent()
}
