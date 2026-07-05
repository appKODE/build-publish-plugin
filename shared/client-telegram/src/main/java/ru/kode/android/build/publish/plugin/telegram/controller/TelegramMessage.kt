package ru.kode.android.build.publish.plugin.telegram.controller

import ru.kode.android.build.publish.plugin.core.entity.IssueSource
import ru.kode.android.build.publish.plugin.telegram.controller.entity.ChatSpecificTelegramBot

data class TelegramMessage(
    val text: String,
    val bots: List<ChatSpecificTelegramBot>,
    val header: String = "",
    val userMentions: List<String> = emptyList(),
    val issueSources: List<IssueSource> = emptyList(),
)
