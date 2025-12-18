package ru.kode.android.build.publish.plugin.confluence.messages

fun servicesCreatedMessage(servicesKeys: Set<String>): String {
    return """
        |
        |============================================================
        |           🚀 CONFLUENCE SERVICES CREATED 🚀
        |============================================================
        | Successfully created Confluence services:
        |
        | ${servicesKeys.joinToString("\n") { "• $it" }}
        |
        | These services will be used for interacting with Confluence API
        | and managing documentation during the build process.
        |============================================================
    """.trimMargin()
}

fun registeringServicesMessage(): String {
    return """
        |
        |============================================================
        |           🔄 REGISTERING CONFLUENCE SERVICES 🔄
        |============================================================
        | Initializing Confluence service registration...
        |
        | This may take a moment as we set up the necessary
        | connections and configurations.
        |============================================================
    """.trimMargin()
}

fun noAuthConfigsMessage(): String {
    return """
        |
        |============================================================
        |           ℹ️ NO CONFLUENCE AUTH CONFIGURATIONS FOUND ℹ️
        |============================================================
        | No Confluence authentication configurations were found.
        | The service map will remain empty and Confluence integration will be disabled.
        |
        | TO ENABLE CONFLUENCE INTEGRATION:
        | 1. Add a 'confluence' configuration block to your build script
        | 2. Configure at least one authentication method
        |
        | Example configuration:
        | confluence {
        |     auth {
        |         common {
        |             baseUrl = "https://your-domain.atlassian.net"
        |             username = providers.gradleProperty("confluence.username")
        |             password = providers.gradleProperty("confluence.apiToken")
        |         }
        |     }
        | }
        |
        | NOTE: Store sensitive credentials in gradle.properties or environment variables.
        |============================================================
    """.trimMargin()
}

fun mustBeUsedWithFoundationPluginException(): String {
    return """
        |
        |============================================================
        |              🚨 PLUGIN CONFIGURATION ERROR 🚨
        |============================================================
        | The Confluence plugin requires the BuildPublishFoundationPlugin
        | to be applied first.
        |
        | REQUIRED ACTION:
        | Add the following to your module's build.gradle.kts file:
        |
        | plugins {
        |     id("ru.kode.android.build-publish-novo.foundation") version "<version>"
        |     id("ru.kode.android.build-publish-novo.confluence") version "<version>"
        | }
        |
        | Make sure to replace <version> with the correct version number.
        |============================================================
    """.trimMargin()
}

fun extensionCreatedMessage(): String {
    return """
        |
        |============================================================
        |           ℹ️ CONFLUENCE EXTENSION INITIALIZED ℹ️
        |============================================================
        | ConfluenceServiceExtension has been created but no configuration
        | has been provided yet.
        |
        | NEXT STEPS:
        | 1. Configure your Confluence settings in the build script
        | 2. Set up authentication and distribution blocks
        | 3. Apply the configuration using the 'confluence' extension
        |============================================================
    """.trimMargin()
}

fun uploadFailedMessage(): String {
    return """
        |
        |============================================================
        |           ⚠️ CONFLUENCE UPLOAD TIMED OUT ⚠️
        |============================================================
        | The Confluence upload operation timed out, but the file may
        | have been uploaded successfully.
        |
        | POSSIBLE REASONS:
        | 1. Network latency or connectivity issues
        | 2. Large file size causing the upload to take longer than expected
        | 3. Confluence server under heavy load
        |
        | RECOMMENDED ACTIONS:
        | 1. Check your Confluence space to verify if the file was uploaded
        | 2. Check your network connection
        | 3. Try uploading the file again if needed
        |============================================================
    """.trimMargin()
}

fun ioExceptionMessage(attempt: Int, delayMillis: Long, maxRetries: Int): String {
    return """
        |
        |============================================================
        |           ⚠️ NETWORK IO ERROR - RETRYING ⚠️
        |============================================================
        | An I/O error occurred while communicating with Confluence.
        |
        | ATTEMPT: ${attempt + 1} of $maxRetries
        | RETRYING IN: ${delayMillis}ms
        |
        | NEXT STEPS:
        | 1. The operation will be retried automatically
        | 2. Check your network connection if this persists
        | 3. Review the full error in the logs for more details
        |============================================================
    """.trimMargin()
}

fun eofDuringHandShakeMessage(attempt: Int, delayMillis: Long, maxRetries: Int): String {
    return """
        |
        |============================================================
        |        ⚠️ CONNECTION ERROR DURING HANDSHAKE ⚠️
        |============================================================
        | The connection to Confluence was terminated during the TLS handshake.
        |
        | ATTEMPT: ${attempt + 1} of $maxRetries
        | RETRYING IN: ${delayMillis}ms
        |
        | POSSIBLE CAUSES:
        | 1. Network connectivity issues
        | 2. Server-side termination of the connection
        | 3. TLS/SSL configuration problems
        |
        | The operation will be retried automatically.
        |============================================================
    """.trimMargin()
}

fun sslHandShakeMessage(attempt: Int, delayMillis: Long, maxRetries: Int): String {
    return """
        |
        |============================================================
        |           ⚠️ SSL HANDSHAKE FAILURE ⚠️
        |============================================================
        | Failed to establish a secure connection to Confluence.
        |
        | ATTEMPT: ${attempt + 1} of $maxRetries
        | RETRYING IN: ${delayMillis}ms
        |
        | POSSIBLE CAUSES:
        | 1. SSL certificate validation failure
        | 2. Outdated TLS configuration
        | 3. Network security restrictions
        |
        | The operation will be retried with additional logging.
        |============================================================
    """.trimMargin()
}

fun needProvideDistributionConfigMessage(variantName: String): String {
    return """
        |
        |============================================================
        |        🚨 MISSING DISTRIBUTION CONFIGURATION 🚨
        |============================================================
        | No distribution configuration found for variant: $variantName
        |
        | REQUIRED ACTION:
        | 1. Add a 'distribution' block to your build script
        | 2. Configure it for '$variantName' or 'common' configuration
        |
        | Example configuration:
        | confluence {
        |     distribution {
        |         $variantName {
        |             spaceKey = "YOUR_SPACE_KEY"
        |             parentPageId = "PARENT_PAGE_ID"
        |             // Other distribution settings...
        |         }
        |     }
        | }
        |
        | NOTE: This configuration is required for Confluence integration.
        |============================================================
    """.trimMargin()
}

fun needProvideAuthConfigMessage(variantName: String): String {
    return """
        |
        |============================================================
        |         🚨 MISSING AUTHENTICATION CONFIGURATION 🚨
        |============================================================
        | No authentication configuration found for variant: $variantName
        |
        | REQUIRED ACTION:
        | 1. Add an 'auth' block to your build script
        | 2. Configure it for '$variantName' or 'common' configuration
        |
        | Example configuration:
        | confluence {
        |     auth {
        |         $variantName {
        |             baseUrl = "https://your-domain.atlassian.net"
        |             username = providers.gradleProperty("confluence.username")
        |             password = providers.gradleProperty("confluence.apiToken")
        |         }
        |     }
        | }
        |
        | SECURITY NOTE:
        | • Store sensitive credentials in gradle.properties or environment variables
        | • Never commit credentials directly in build files
        |• Consider using API tokens instead of passwords
        |============================================================
    """.trimMargin()
}
