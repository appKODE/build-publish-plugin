package ru.kode.android.build.publish.plugin.clickup.messages

import ru.kode.android.build.publish.plugin.clickup.EXTENSION_NAME

fun servicesCreatedMessage(serviceMap: Set<String>): String {
    return """

        |============================================================
        |            CLICKUP SERVICES CREATED SUCCESSFULLY
        |============================================================
        | Successfully created and configured ClickUp services:
        |
        | ${serviceMap.joinToString(", ")}
        |
        | These services will be used for interacting with ClickUp API
        | and managing tasks and custom fields during the build process.
        |============================================================
        """.trimIndent()
}

fun registeringServicesMessage(): String {
    return """

        |============================================================
        |                REGISTERING CLICKUP SERVICES
        |============================================================
        | Initializing ClickUp service registration...
        |
        | This may take a moment as we set up the necessary
        | connections and configurations.
        |============================================================
        """.trimIndent()
}

fun noAuthConfigMessage(): String {
    return """

        |============================================================
        |             NO CLICKUP AUTH CONFIGURATIONS FOUND
        |============================================================
        | No ClickUp authentication configurations were found.
        | The service map will remain empty and ClickUp integration
        | will be disabled.
        |
        | TO ENABLE CLICKUP INTEGRATION:
        |  1. Add a 'clickup' configuration block to your build script
        |  2. Configure at least one authentication method
        |
        | EXAMPLE CONFIGURATION:
        |
        | $EXTENSION_NAME {
        |    auth {
        |        common {
        |            apiTokenFile = File("clickup-token.txt")
        |        }
        |    }
        |  }
        |
        | SECURITY NOTE:
        | Store sensitive credentials in local.properties
        | or environment variables.
        |============================================================
        """.trimIndent()
}

fun mustApplyFoundationPluginMessage(): String {
    return """

        |============================================================
        |                 PLUGIN CONFIGURATION ERROR
        |============================================================
        | The ClickUp plugin requires the BuildPublishFoundationPlugin
        | to be applied first.
        |
        | REQUIRED ACTION:
        | Add the following to your module's build.gradle.kts file:
        |
        | plugins {
        |     id("ru.kode.android.build-publish-novo.foundation") version "<version>"
        |     id("ru.kode.android.build-publish-novo.clickup") version "<version>"
        | }
        |
        | Make sure to replace <version> with the correct version number.
        |============================================================
        """.trimIndent()
}

fun extensionCreatedMessage(): String {
    return """

        |============================================================
        |               CLICKUP EXTENSION INITIALIZED
        |============================================================
        | ClickUp extensions has been created but no configuration
        | has been provided yet.
        |
        | NEXT STEPS:
        |  1. Configure your ClickUp settings in the build script
        |  2. Set up auth and automation blocks
        |  3. Apply the configuration using the '$EXTENSION_NAME'
        |     extension
        |============================================================
        """.trimIndent()
}

fun propertiesNotAppliedMessage(): String {
    return """

        |============================================================
        |                MISSING REQUIRED PROPERTIES
        |============================================================
        | To use the fixVersion logic,
        | the following properties must be specified:
        |
        | REQUIRED:
        | • fixVersionPattern - The pattern to build fix version
        | • fixVersionFieldName - The name of the custom field
        |   in ClickUp
        |
        | EXAMPLE CONFIGURATION:
        |
        | $EXTENSION_NAME {
        |     automation {
        |         fixVersionPattern = "v?\\d+\\.\\d+\\.\\d+"
        |         fixVersionFieldName = "customfield_12345"
        |     }
        | }
        |
        | NOTE: These properties are required for version automation.
        |============================================================
        """.trimIndent()
}

fun issuesNotFoundMessage(): String {
    return """

        |============================================================
        |               NO ISSUES FOUND IN CHANGELOG
        |============================================================
        | No issue references were found in the changelog.
        | No automation will be performed for this build.
        |
        | If you expected to see ClickUp issues updated, please check:
        |  1. Your commit messages include task references (e.g., #123)
        |  2. The changelog format includes the issue references
        |  3. The task references match your ClickUp task IDs
        |============================================================
        """.trimIndent()
}

fun provideAutomationConfigMessage(variantName: String): String {
    return """

        |============================================================
        |             MISSING AUTOMATION CONFIGURATION
        |============================================================
        | No ClickUp automation configuration found for variant:
        | $variantName
        |
        | REQUIRED ACTION:
        |  1. Add an 'automation' block to your build script
        |  2. Configure it for '$variantName' or 'common'
        |     configuration
        |
        | EXAMPLE CONFIGURATION:
        |
        | $EXTENSION_NAME {
        |     automation {
        |         common { // or buildVariant($variantName)
        |             // Your automation settings...
        |         }
        |     }
        | }
        |
        | For more details, check the plugin documentation.
        |============================================================
        """.trimIndent()
}

fun unknownAccountNameMessage(
    accountName: String,
    availableAccountNames: Collection<String>,
): String {
    return """

        |============================================================
        |               UNKNOWN CLICKUP ACCOUNT
        |============================================================
        | No ClickUp account named '$accountName' is declared.
        |
        | AVAILABLE ACCOUNTS: ${availableAccountNames.joinToString(", ").ifBlank { "(none)" }}
        |
        | Declare it under the auth block:
        |   auth { common { account("$accountName") { apiTokenFile.set(...) } } }
        |============================================================
        """.trimIndent()
}

fun unknownProjectNameMessage(
    projectName: String,
    accountName: String,
    availableProjectNames: Collection<String>,
): String {
    return """

        |============================================================
        |               UNKNOWN CLICKUP PROJECT
        |============================================================
        | No project named '$projectName' is declared on account '$accountName'.
        |
        | AVAILABLE PROJECTS: ${availableProjectNames.joinToString(", ").ifBlank { "(none)" }}
        |
        | Declare it under the account:
        |   account("$accountName") { project("$projectName") { workspaceName.set(...); taskIdPrefix.set(...) } }
        |============================================================
        """.trimIndent()
}

fun duplicateTaskIdPrefixMessage(taskIdPrefix: String): String {
    return """

        |============================================================
        |             DUPLICATE CLICKUP TASK-ID PREFIX
        |============================================================
        | The task-id prefix '$taskIdPrefix' is declared on more than one project.
        | Prefixes route changelog references to a single account/workspace, so
        | each 'taskIdPrefix' must be globally unique across all accounts.
        |============================================================
        """.trimIndent()
}

fun provideAuthConfigMessage(variantName: String): String {
    return """

        |============================================================
        |           MISSING AUTHENTICATION CONFIGURATION
        |============================================================
        | No ClickUp authentication configuration found for variant:
        | $variantName
        |
        | REQUIRED ACTION:
        |  1. Add an 'auth' block to your build script
        |  2. Configure it for '$variantName' or 'common' configuration
        |
        | EXAMPLE CONFIGURATION:
        |
        | $EXTENSION_NAME {
        |     auth {
        |         common { // or buildVariant($variantName)
        |             // Your auth settings...
        |         }
        |     }
        | }
        |
        | SECURITY NOTE:
        | • Store sensitive credentials in gradle.properties or
        |   environment variables
        | • Never commit credentials directly in build files
        |============================================================
        """.trimIndent()
}
