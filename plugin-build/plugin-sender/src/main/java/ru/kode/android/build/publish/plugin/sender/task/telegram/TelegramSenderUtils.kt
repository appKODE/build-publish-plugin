package ru.kode.android.build.publish.plugin.sender.task.telegram

import ru.kode.android.build.publish.plugin.telegram.controller.entity.ChatSpecificTelegramBot

internal fun buildSenderBot(
    botId: String,
    chatId: String,
    topicId: String,
    serverBaseUrl: String,
): ChatSpecificTelegramBot =
    ChatSpecificTelegramBot(
        name = "sender",
        id = botId,
        serverBaseUrl = serverBaseUrl.takeIf { it.isNotEmpty() },
        basicAuth = null,
        chatId = chatId,
        topicId = topicId.takeIf { it.isNotEmpty() },
    )
