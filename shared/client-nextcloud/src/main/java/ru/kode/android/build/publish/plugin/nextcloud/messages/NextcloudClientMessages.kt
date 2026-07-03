package ru.kode.android.build.publish.plugin.nextcloud.messages

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
