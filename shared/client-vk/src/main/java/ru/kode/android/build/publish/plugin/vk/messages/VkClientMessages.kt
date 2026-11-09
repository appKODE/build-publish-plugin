package ru.kode.android.build.publish.plugin.vk.messages

fun methodFailedMessage(
    method: String,
    errorCode: Int?,
    errorMessage: String?,
): String =
    """

    |============================================================
    |                    VK API METHOD FAILED
    |============================================================
    | VK API method '$method' returned an error.
    |
    |  • Error code: $errorCode
    |  • Error message: $errorMessage
    |
    | 5: the access token is invalid.
    | 15, 27: the token lacks the 'docs' or 'messages' right.
    | 901: the user has not allowed messages from the community.
    | 917: the community is not a member of the chat.
    |============================================================
    """.trimIndent()

fun httpFailedMessage(
    path: String,
    statusCode: Int,
    body: String,
): String =
    """

    |============================================================
    |                    VK REQUEST FAILED
    |============================================================
    | Request to '$path' failed.
    |
    |  • Status code: $statusCode
    |  • Response: $body
    |============================================================
    """.trimIndent()

fun uploadFailedMessage(
    error: String?,
    description: String?,
): String =
    """

    |============================================================
    |                  VK DOCUMENT UPLOAD FAILED
    |============================================================
    | The VK upload server did not accept the file.
    |
    |  • Error: $error
    |  • Description: $description
    |============================================================
    """.trimIndent()
