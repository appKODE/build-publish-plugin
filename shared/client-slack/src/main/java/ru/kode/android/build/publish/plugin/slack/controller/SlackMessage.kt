package ru.kode.android.build.publish.plugin.slack.controller

import ru.kode.android.build.publish.plugin.core.enity.IssueSource

data class SlackMessage(
    val webhookUrl: String,
    val text: String,
    val header: String = "",
    val userMentions: List<String> = emptyList(),
    val iconUrl: String = "",
    val attachmentColor: String = "#36a64f",
    val issueSources: List<IssueSource> = emptyList(),
)
