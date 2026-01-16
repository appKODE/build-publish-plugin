package ru.kode.android.build.publish.plugin.telegram.network.entity

import kotlinx.serialization.Serializable

/**
 * Telegram chat model used in message/update responses.
 */
@Serializable
internal data class TelegramChat(
    val id: Long,
    val title: String,
)
