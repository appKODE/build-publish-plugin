package ru.kode.android.build.publish.plugin.confluence.messages

fun sslHandshakeRetryMessage(
    attempt: Int,
    maxRetries: Int,
    delayMillis: Long,
): String = "SSL handshake failed (attempt $attempt/$maxRetries), retrying in ${delayMillis}ms"

fun eofHandshakeRetryMessage(
    attempt: Int,
    maxRetries: Int,
    delayMillis: Long,
): String = "EOF during handshake (attempt $attempt/$maxRetries), retrying in ${delayMillis}ms"

fun ioExceptionRetryMessage(
    attempt: Int,
    maxRetries: Int,
    delayMillis: Long,
): String = "IO exception (attempt $attempt/$maxRetries), retrying in ${delayMillis}ms"

fun uploadFileFailedMessage(
    fileName: String,
    pageId: String,
): String {
    return """

        |============================================================
        |                 FAILED TO UPLOAD ATTACHMENT
        |============================================================
        | Could not upload '$fileName' to Confluence page $pageId
        |
        | POSSIBLE CAUSES:
        |  1. The page id might not exist or be inaccessible
        |  2. Insufficient permissions to add attachments
        |  3. The file is empty, missing or too large
        |
        | TROUBLESHOOTING:
        |  1. Verify the page id and your Confluence permissions
        |  2. Check the file exists and is readable
        |  3. Review the full error in the logs
        |============================================================
        """.trimIndent().trim()
}

fun addCommentFailedMessage(
    pageId: String,
    fileName: String,
): String {
    return """

        |============================================================
        |                   FAILED TO ADD COMMENT
        |============================================================
        | Could not add a comment for '$fileName' to Confluence page $pageId
        |
        | POSSIBLE CAUSES:
        |  1. The page id might not exist or be inaccessible
        |  2. Insufficient permissions to comment on the page
        |
        | TROUBLESHOOTING:
        |  1. Verify the page id and your Confluence permissions
        |  2. Review the full error in the logs
        |============================================================
        """.trimIndent().trim()
}

fun removeAttachmentFailedMessage(attachmentId: String): String {
    return """

        |============================================================
        |                FAILED TO REMOVE ATTACHMENT
        |============================================================
        | Could not remove attachment $attachmentId
        |
        | POSSIBLE CAUSES:
        |  1. The attachment might have already been removed
        |  2. Insufficient permissions to delete content
        |
        | TROUBLESHOOTING:
        |  1. Verify the attachment id and your Confluence permissions
        |  2. Review the full error in the logs
        |============================================================
        """.trimIndent().trim()
}

fun removeCommentFailedMessage(commentId: String): String {
    return """

        |============================================================
        |                 FAILED TO REMOVE COMMENT
        |============================================================
        | Could not remove comment $commentId
        |
        | POSSIBLE CAUSES:
        |  1. The comment might have already been removed
        |  2. Insufficient permissions to delete content
        |
        | TROUBLESHOOTING:
        |  1. Verify the comment id and your Confluence permissions
        |  2. Review the full error in the logs
        |============================================================
        """.trimIndent().trim()
}
