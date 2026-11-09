package ru.kode.android.build.publish.plugin.vk.messages

import ru.kode.android.build.publish.plugin.vk.EXTENSION_NAME

fun servicesCreatedMessage(servicesKeys: Set<String>): String =
    """

    |============================================================
    |                VK SERVICES CREATED
    |============================================================
    | Successfully created VK services:
    |
    | ${servicesKeys.joinToString("\n") { "• $it" }}
    |
    |============================================================
    """.trimIndent()

fun registeringServicesMessage(): String =
    """

    |============================================================
    |              REGISTERING VK SERVICES
    |============================================================
    | Initializing VK service registration...
    |============================================================
    """.trimIndent()

fun noAuthConfigsMessage(): String =
    """

    |============================================================
    |          NO VK AUTH CONFIGURATIONS FOUND
    |============================================================
    | No VK authentication configurations were found.
    | Integration is disabled.
    |
    | Configure at least one auth block in `$EXTENSION_NAME`.
    |============================================================
    """.trimIndent()

fun foundationPluginNotFoundException(): String =
    """

    |============================================================
    |                 PLUGIN CONFIGURATION ERROR
    |============================================================
    | The VK plugin requires foundation plugin.
    |
    | plugins {
    |     id("ru.kode.android.build-publish-novo.foundation")
    |     id("ru.kode.android.build-publish-novo.vk")
    | }
    |============================================================
    """.trimIndent()

fun uploadFailedMessage(): String =
    """

    |============================================================
    |              VK DISTRIBUTION FAILED
    |============================================================
    | Uploading or sending the document failed.
    |============================================================
    """.trimIndent()

fun documentSentMessage(
    peerId: Long,
    messageId: Long,
    documentUrl: String?,
): String =
    """

    |============================================================
    |                    VK DOCUMENT SENT
    |============================================================
    | Document sent to peer $peerId (message id $messageId)
    |  • URL: ${documentUrl ?: "not returned"}
    |============================================================
    """.trimIndent()

fun needProvideDistributionConfigMessage(variantName: String): String =
    """

    |============================================================
    |            MISSING DISTRIBUTION CONFIGURATION
    |============================================================
    | No distribution configuration found for variant: $variantName
    |
    | Configure `$EXTENSION_NAME { distribution { ... } }` with
    | `common { ... }` or `buildVariant("$variantName") { ... }`.
    |============================================================
    """.trimIndent()

fun needProvideAuthConfigMessage(variantName: String): String =
    """

    |============================================================
    |            MISSING AUTHENTICATION CONFIGURATION
    |============================================================
    | No authentication configuration found for variant: $variantName
    |
    | Configure `$EXTENSION_NAME { auth { ... } }` with
    | `common { ... }` or `buildVariant("$variantName") { ... }`.
    |============================================================
    """.trimIndent()
