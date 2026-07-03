package ru.kode.android.build.publish.plugin.slack.controller

import java.io.File

interface SlackController {
    fun send(message: SlackMessage)

    fun upload(
        uploadToken: String,
        initialComment: String,
        file: File,
        channels: List<String>,
    )
}
