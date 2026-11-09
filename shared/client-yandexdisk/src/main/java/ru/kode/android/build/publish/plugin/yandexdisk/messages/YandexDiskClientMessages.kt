package ru.kode.android.build.publish.plugin.yandexdisk.messages

fun requestFailedMessage(
    path: String,
    statusCode: Int,
    body: String,
): String =
    """

    |============================================================
    |               YANDEX DISK REQUEST FAILED
    |============================================================
    | Request to '$path' failed.
    |
    |  • Status code: $statusCode
    |  • Response: $body
    |
    | 401 means the OAuth token is invalid or expired,
    | 507 means there is not enough free space on the disk.
    |============================================================
    """.trimIndent()

fun publicUrlMissingMessage(path: String): String =
    """

    |============================================================
    |             YANDEX DISK PUBLIC LINK MISSING
    |============================================================
    | '$path' was published but Yandex Disk returned no public_url.
    |============================================================
    """.trimIndent()

fun blankRemotePathMessage(): String =
    """

    |============================================================
    |              YANDEX DISK REMOTE PATH IS BLANK
    |============================================================
    | remotePath must point to a folder, for example 'builds/android'.
    |============================================================
    """.trimIndent()
