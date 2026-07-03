package ru.kode.android.build.publish.plugin.nextcloud.messages

import ru.kode.android.build.publish.plugin.nextcloud.EXTENSION_NAME

fun uploadingToNextcloudMessage(
    fileName: String,
    remotePath: String,
    remoteFileName: String,
): String = "Uploading $fileName to Nextcloud at $remotePath/$remoteFileName"

fun servicesCreatedMessage(servicesKeys: Set<String>): String {
    return """

        |============================================================
        |                NEXTCLOUD SERVICES CREATED
        |============================================================
        | Successfully created Nextcloud services:
        |
        | ${servicesKeys.joinToString("\n") { "• $it" }}
        |
        |============================================================
        """.trimIndent()
}

fun registeringServicesMessage(): String {
    return """

        |============================================================
        |              REGISTERING NEXTCLOUD SERVICES
        |============================================================
        | Initializing Nextcloud service registration...
        |============================================================
        """.trimIndent()
}

fun noAuthConfigsMessage(): String {
    return """

        |============================================================
        |          NO NEXTCLOUD AUTH CONFIGURATIONS FOUND
        |============================================================
        | No Nextcloud authentication configurations were found.
        | Integration is disabled.
        |
        | Configure at least one auth block in `$EXTENSION_NAME`.
        |============================================================
        """.trimIndent()
}

fun foundationPluginNotFoundException(): String {
    return """

        |============================================================
        |                 PLUGIN CONFIGURATION ERROR
        |============================================================
        | The Nextcloud plugin requires foundation plugin.
        |
        | plugins {
        |     id("ru.kode.android.build-publish-novo.foundation")
        |     id("ru.kode.android.build-publish-novo.nextcloud")
        | }
        |============================================================
        """.trimIndent()
}

fun uploadFailedMessage(): String {
    return """

        |============================================================
        |              NEXTCLOUD DISTRIBUTION FAILED
        |============================================================
        | Upload/share operation failed.
        |============================================================
        """.trimIndent()
}

fun shareCreatedMessage(
    remoteFilePath: String,
    url: String,
): String {
    return """

        |============================================================
        |                NEXTCLOUD SHARE CREATED
        |============================================================
        | Nextcloud public share created for:
        |  • Path: $remoteFilePath
        |  • URL: $url
        |============================================================
        """.trimIndent()
}

fun shareReusedMessage(
    remoteFilePath: String,
    url: String,
): String {
    return """

        |============================================================
        |                NEXTCLOUD SHARE REUSED
        |============================================================
        | Nextcloud public share reused for:
        |  • Path: $remoteFilePath
        |  • URL: $url
        |============================================================
        """.trimIndent()
}

fun internalShareReadyMessage(
    remoteFilePath: String,
    internalUrl: String,
    createdCount: Int,
    reusedCount: Int,
): String {
    return """

        |============================================================
        |             NEXTCLOUD INTERNAL SHARE READY
        |============================================================
        | Internal share successfully prepared for:
        |  • Path: $remoteFilePath
        |  • URL: $internalUrl
        |
        | Statistics:
        |  • Created: $createdCount
        |  • Reused: $reusedCount
        |============================================================
        """.trimIndent()
}

fun needProvideDistributionConfigMessage(variantName: String): String {
    return """

        |============================================================
        |            MISSING DISTRIBUTION CONFIGURATION
        |============================================================
        | No distribution configuration found for variant: $variantName
        |
        | Configure `$EXTENSION_NAME { distribution { ... } }` with
        | `common { ... }` or `buildVariant("$variantName") { ... }`.
        |============================================================
        """.trimIndent()
}

fun needProvideAuthConfigMessage(variantName: String): String {
    return """

        |============================================================
        |            MISSING AUTHENTICATION CONFIGURATION
        |============================================================
        | No authentication configuration found for variant: $variantName
        |
        | Configure `$EXTENSION_NAME { auth { ... } }` with
        | `common { ... }` or `buildVariant("$variantName") { ... }`.
        |============================================================
        """.trimIndent()
}
