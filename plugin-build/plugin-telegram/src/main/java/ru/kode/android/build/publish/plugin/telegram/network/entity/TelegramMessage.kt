package ru.kode.android.build.publish.plugin.telegram.network.entity

import kotlinx.serialization.Serializable

@Serializable
internal data class TelegramMessage(
    val message_id: Long,
    val chat: TelegramChat,
    val date: Long,
    val text: String?
)
