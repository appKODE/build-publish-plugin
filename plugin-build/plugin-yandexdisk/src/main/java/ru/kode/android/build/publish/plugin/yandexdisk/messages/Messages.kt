package ru.kode.android.build.publish.plugin.yandexdisk.messages

import ru.kode.android.build.publish.plugin.yandexdisk.EXTENSION_NAME

fun uploadingToYandexDiskMessage(
    fileName: String,
    remotePath: String,
    remoteFileName: String,
): String = "Uploading $fileName to Yandex Disk at $remotePath/$remoteFileName"

fun servicesCreatedMessage(servicesKeys: Set<String>): String =
    """

    |============================================================
    |                YANDEX DISK SERVICES CREATED
    |============================================================
    | Successfully created Yandex Disk services:
    |
    | ${servicesKeys.joinToString("\n") { "• $it" }}
    |
    |============================================================
    """.trimIndent()

fun registeringServicesMessage(): String =
    """

    |============================================================
    |              REGISTERING YANDEX DISK SERVICES
    |============================================================
    | Initializing Yandex Disk service registration...
    |============================================================
    """.trimIndent()

fun noAuthConfigsMessage(): String =
    """

    |============================================================
    |          NO YANDEX DISK AUTH CONFIGURATIONS FOUND
    |============================================================
    | No Yandex Disk authentication configurations were found.
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
    | The Yandex Disk plugin requires foundation plugin.
    |
    | plugins {
    |     id("ru.kode.android.build-publish-novo.foundation")
    |     id("ru.kode.android.build-publish-novo.yandexdisk")
    | }
    |============================================================
    """.trimIndent()

fun uploadFailedMessage(): String =
    """

    |============================================================
    |              YANDEX DISK DISTRIBUTION FAILED
    |============================================================
    | Upload or publish operation failed.
    |============================================================
    """.trimIndent()

fun shareCreatedMessage(
    remoteFilePath: String,
    url: String,
): String =
    """

    |============================================================
    |                YANDEX DISK SHARE CREATED
    |============================================================
    | Yandex Disk public share created for:
    |  • Path: $remoteFilePath
    |  • URL: $url
    |============================================================
    """.trimIndent()

fun shareReusedMessage(
    remoteFilePath: String,
    url: String,
): String =
    """

    |============================================================
    |                YANDEX DISK SHARE REUSED
    |============================================================
    | Yandex Disk public share reused for:
    |  • Path: $remoteFilePath
    |  • URL: $url
    |============================================================
    """.trimIndent()

fun fileUploadedMessage(remoteFilePath: String): String =
    """

    |============================================================
    |                 YANDEX DISK UPLOAD FINISHED
    |============================================================
    | File uploaded to disk:/$remoteFilePath
    | It is private; set publicLink = true to get a public link.
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
