package ru.kode.android.build.publish.plugin.slack.task.distribution.entity

internal interface BaseSlackResponse {
    val ok: Boolean
    val error: String?
}
