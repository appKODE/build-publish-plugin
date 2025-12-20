package ru.kode.android.build.publish.plugin.clickup.messages

import ru.kode.android.build.publish.plugin.clickup.EXTENSION_NAME
import ru.kode.android.build.publish.plugin.clickup.controller.entity.ClickUpList
import ru.kode.android.build.publish.plugin.clickup.controller.entity.ClickUpSpace
import ru.kode.android.build.publish.plugin.clickup.controller.entity.ClickUpTeam

fun servicesCreatedMessage(serviceMap: Set<String>): String {
    return """
        
        |============================================================
        |         🚀 CLICKUP SERVICES CREATED SUCCESSFULLY 🚀
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
        |             🔄 REGISTERING CLICKUP SERVICES 🔄
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
        |          ℹ️ NO CLICKUP AUTH CONFIGURATIONS FOUND ℹ️
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
        |              🚨 PLUGIN CONFIGURATION ERROR 🚨
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
        |            ℹ️ CLICKUP EXTENSION INITIALIZED ℹ️
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
        |             ⚠️ MISSING REQUIRED PROPERTIES ⚠️
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
        |            ℹ️ NO ISSUES FOUND IN CHANGELOG ℹ️
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
        |          ⚠️ MISSING AUTOMATION CONFIGURATION ⚠️
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

fun provideAuthConfigMessage(variantName: String): String {
    return """
        
        |============================================================
        |        ⚠️ MISSING AUTHENTICATION CONFIGURATION ⚠️
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

fun failedAddFieldMessage(
    fieldId: String,
    taskId: String,
): String {
    return """
        
        |============================================================
        |            ⚠️ FAILED TO ADD FIELD TO TASK ⚠️
        |============================================================
        | Field ID: $fieldId
        | Task ID: $taskId
        |
        | POSSIBLE CAUSES:
        |  1. Invalid field ID or task ID
        |  2. Insufficient permissions to modify the task
        |  3. The field might not exist in the task's list/space
        |
        | RECOMMENDED ACTIONS:
        |  1. Verify the field ID is correct
        |  2. Check your ClickUp permissions for this task
        |  3. Review the full error in the logs
        |============================================================
        """.trimIndent()
}

fun customFieldClearedMessage(
    fieldId: String,
    taskId: String,
): String {
    return """
        
        |============================================================
        |          ✅ CUSTOM FIELD CLEARED SUCCESSFULLY ✅
        |============================================================
        | Field ID: $fieldId
        | Task ID: $taskId
        |
        | The custom field has been successfully cleared.
        |============================================================
        """.trimIndent()
}

fun tagRemovedMessage(
    tagName: String,
    taskId: String,
): String {
    return """
        
        |============================================================
        |          ✅ TAG REMOVED FROM TASK SUCCESSFULLY ✅
        |============================================================
        | Tag: $tagName
        | Task ID: $taskId
        |
        | The tag has been successfully removed from the task.
        |============================================================
        """.trimIndent()
}

fun failedToRemoveTagMessage(
    tagName: String,
    taskId: String,
): String {
    return """
        
        |============================================================
        |           ⚠️ FAILED TO REMOVE TAG FROM TASK ⚠️
        |============================================================
        | Tag: $tagName
        | Task ID: $taskId
        |
        | POSSIBLE CAUSES:
        |  1. The tag might not exist on the task
        |  2. Insufficient permissions to modify the task
        |  3. The task might be in a read-only state
        |
        | RECOMMENDED ACTIONS:
        |  1. Verify the tag exists on the task
        |  2. Check your ClickUp permissions
        |  3. Try removing the tag manually in ClickUp
        |  4. Review the full error in the logs
        |============================================================
        """.trimIndent()
}

fun failedAddTagMessage(
    tagName: String,
    taskId: String,
): String {
    return """
        
        |============================================================
        |             ⚠️ FAILED TO ADD TAG TO TASK ⚠️
        |============================================================
        | Tag: $tagName
        | Task ID: $taskId
        |
        | POSSIBLE CAUSES:
        |  1. The tag name might be invalid or too long
        |  2. Insufficient permissions to modify the task
        |  3. The task might be in a read-only state
        |
        | TROUBLESHOOTING:
        |  1. Verify the tag name follows ClickUp's requirements
        |  2. Check your ClickUp permissions for this task
        |  3. Try adding the tag manually in ClickUp
        |  4. Review the full error in the logs
        |============================================================
        """.trimIndent()
}

fun failedToDeleteCustomFieldMessage(
    fieldId: String,
    list: ClickUpList,
): String {
    return """
        
        |============================================================
        |       ⚠️ FAILED TO DELETE CUSTOM FIELD FROM LIST ⚠️
        |============================================================
        | Field ID: $fieldId
        | List ID: ${list.id}
        |
        | POSSIBLE CAUSES:
        |  1. The field might be in use by tasks
        |  2. Insufficient permissions to modify the list
        |  3. The field might be required by the list
        |
        | RECOMMENDED ACTIONS:
        |  1. Check if the field is used by any tasks
        |  2. Verify your ClickUp permissions
        |  3. Try deleting the field manually in ClickUp
        |  4. Review the full error in the logs
        |============================================================
        """.trimIndent()
}

fun listNotFoundForDeleteMessage(
    space: ClickUpSpace,
    fieldId: String,
): String {
    return """
        
        |============================================================
        |               ⚠️ LIST NOT FOUND IN SPACE ⚠️
        |============================================================
        | Space: ${space.name} (ID: ${space.id})
        | Field ID: $fieldId
        |
        | No list was found in the specified space where the custom 
        | field could be deleted from.
        |
        | POSSIBLE CAUSES:
        |  1. The space might be empty
        |  2. The lists might have been deleted
        |  3. You might not have permission to view the lists
        |
        | REQUIRED ACTION:
        |  1. Check the space in ClickUp web interface
        |  2. Create a list in the space if needed
        |  3. Verify your permissions for this space
        |
        | NOTE: 
        | You need to have at least one list in the space to manage 
        | custom fields.
        |============================================================
        """.trimIndent()
}

fun teamNotFoundForDeleteMessage(
    team: ClickUpTeam,
    fieldId: String,
): String {
    return """
        
        |============================================================
        |              ⚠️ SPACE NOT FOUND IN TEAM ⚠️
        |============================================================
        | Team: ${team.name} (ID: ${team.id})
        | Field ID: $fieldId
        |
        | No space was found in the specified team where the custom 
        | field could be deleted from.
        |
        | POSSIBLE CAUSES:
        |  1. The team might not have any spaces
        |  2. The spaces might have been deleted
        |  3. You might not have permission to view the spaces
        |
        | REQUIRED ACTION:
        |  1. Check the team in ClickUp web interface
        |  2. Create a space in the team if needed
        |  3. Verify your permissions for this team
        |
        | NOTE: 
        | You need to have at least one space in the team to manage 
        | custom fields.
        |============================================================
        """.trimIndent()
}

fun teamNotFoundForDeleteMessage(workspaceName: String): String {
    return """
        
        |============================================================
        |           ⚠       ️ TEAM NOT FOUND ⚠️
        |============================================================
        | Workspace: $workspaceName
        |
        | The specified team/workspace was not found in your ClickUp 
        | account.
        |
        | POSSIBLE CAUSES:
        |  1. The workspace name might be misspelled
        |  2. You might not have access to this workspace
        |  3. The workspace might have been deleted or renamed
        |
        | REQUIRED ACTION:
        |  1. Verify the workspace name is correct
        |  2. Check your ClickUp account for the correct workspace name
        |  3. Ensure you have access to the workspace
        |============================================================
        """.trimIndent()
}

fun listNotFoundForCreateMessage(
    team: ClickUpTeam,
    workspaceName: String,
): String {
    return """
        
        |============================================================
        |               ⚠️ LIST NOT FOUND IN SPACE ⚠️
        |============================================================
        | Team: ${team.name} (ID: ${team.id})
        | Workspace: $workspaceName
        |
        | No list was found in the specified space where the custom 
        | field could be created in.
        |
        | POSSIBLE CAUSES:
        |  1. The space might be empty
        |  2. The lists might have been deleted
        |  3. You might not have permission to view the lists
        |
        | REQUIRED ACTION:
        |  1. Check the space in ClickUp web interface
        |  2. Create a list in the space if needed
        |  3. Verify your permissions for this space
        |
        | NOTE: 
        | You need to have at least one list in the space to create 
        | custom fields.
        |============================================================
        """.trimIndent()
}

fun spaceNotFoundForCreateMessage(
    team: ClickUpTeam,
    workspaceName: String,
): String {
    return """
        
        |============================================================
        |              ⚠️ SPACE NOT FOUND IN TEAM ⚠️
        |============================================================
        | Team: ${team.name} (ID: ${team.id})
        | Workspace: $workspaceName
        |
        | No space was found in the specified team where the custom 
        | field could be created in.
        |
        | POSSIBLE CAUSES:
        |  1. The team might not have any spaces
        |  2. The spaces might have been deleted
        |  3. You might not have permission to view the spaces
        |
        | REQUIRED ACTION:
        |  1. Check the team in ClickUp web interface
        |  2. Create a space in the team if needed
        |  3. Verify your permissions for this team
        |
        | NOTE: 
        | You need to have at least one space in the team to create 
        | custom fields.
        |============================================================
        """.trimIndent()
}

fun teamNotFoundForCreateMessage(workspaceName: String): String {
    return """
        
        |============================================================
        |                   ⚠️ TEAM NOT FOUND ⚠️
        |============================================================
        | Workspace: $workspaceName
        |
        | The specified team/workspace was not found in your ClickUp 
        | account.
        |
        | POSSIBLE CAUSES:
        |  1. The workspace name might be misspelled
        |  2. You might not have access to this workspace
        |  3. The workspace might have been deleted or renamed
        |
        | REQUIRED ACTION:
        |  1. Verify the workspace name is correct
        |  2. Check your ClickUp account for the correct workspace 
        |     name
        |  3. Ensure you have access to the workspace
        |============================================================
        """.trimIndent()
}
