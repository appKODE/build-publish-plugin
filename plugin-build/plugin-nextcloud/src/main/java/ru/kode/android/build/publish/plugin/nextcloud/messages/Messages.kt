package ru.kode.android.build.publish.plugin.nextcloud.messages

import ru.kode.android.build.publish.plugin.nextcloud.EXTENSION_NAME

fun noInternalRecipientsConfiguredMessage(): String {
    return """

        |============================================================
        |            MISSING INTERNAL RECIPIENTS ERROR
        |============================================================
        | Nextcloud INTERNAL_RECIPIENTS mode requires at least one
        | userRecipients/groupRecipients entry.
        |============================================================
        """.trimIndent()
}

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

fun cannotCreateShareMessage(
    path: String,
    statusCode: Int,
    statusMessage: String,
): String {
    return """

        |============================================================
        |              CANNOT CREATE NEXTCLOUD SHARE
        |============================================================
        | Failed to create public share for '$path'.
        |
        |  • Status code: $statusCode
        |  • Status message: $statusMessage
        |============================================================
        """.trimIndent()
}

fun cannotCreateTypedShareMessage(
    path: String,
    shareType: Int,
    shareWith: String,
    statusCode: Int,
    statusMessage: String,
): String {
    return """

        |============================================================
        |              CANNOT CREATE NEXTCLOUD SHARE
        |============================================================
        | Failed to create typed share for '$path'.
        |
        |  • Share type: $shareType
        |  • Share with: $shareWith
        |  • Status code: $statusCode
        |  • Status message: $statusMessage
        |============================================================
        """.trimIndent()
}

fun cannotGetSharesMessage(
    path: String,
    statusCode: Int,
    statusMessage: String,
): String {
    return """

        |============================================================
        |               CANNOT GET NEXTCLOUD SHARES
        |============================================================
        | Failed to query shares for '$path'.
        |
        |  • Status code: $statusCode
        |  • Status message: $statusMessage
        |============================================================
        """.trimIndent()
}

fun cannotResolveInternalLinkFileIdMessage(path: String): String {
    return """

        |============================================================
        |             CANNOT RESOLVE NEXTCLOUD FILE ID
        |============================================================
        | Cannot resolve Nextcloud internal link file id for '$path'
        | from OCS share responses.
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

fun ioExceptionMessage(
    attempt: Int,
    delayMillis: Long,
    maxRetries: Int,
): String {
    return """

        |============================================================
        |                   NEXTCLOUD I/O ERROR
        |============================================================
        | I/O error while calling Nextcloud.
        |
        |  • Attempt: ${attempt + 1}/$maxRetries
        |  • Next retry in: ${delayMillis}ms
        |============================================================
        """.trimIndent()
}

fun eofDuringHandShakeMessage(
    attempt: Int,
    delayMillis: Long,
    maxRetries: Int,
): String {
    return """

        |============================================================
        |                NEXTCLOUD HANDSHAKE ERROR
        |============================================================
        | TLS EOF during Nextcloud handshake.
        |
        |  • Attempt: ${attempt + 1}/$maxRetries
        |  • Next retry in: ${delayMillis}ms
        |============================================================
        """.trimIndent()
}

fun sslHandShakeMessage(
    attempt: Int,
    delayMillis: Long,
    maxRetries: Int,
): String {
    return """

        |============================================================
        |                   NEXTCLOUD SSL ERROR
        |============================================================
        | SSL handshake failed for Nextcloud.
        |
        |  • Attempt: ${attempt + 1}/$maxRetries
        |  • Next retry in: ${delayMillis}ms
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
