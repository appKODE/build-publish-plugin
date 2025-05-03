package ru.kode.android.build.publish.plugin.task.slack.distribution.entity

interface BaseSlackResponse {
    val ok: Boolean
    val error: String?
}
