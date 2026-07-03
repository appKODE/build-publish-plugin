package ru.kode.android.build.publish.plugin.telegram.network.entity

import kotlinx.serialization.Serializable

/**
 * Telegram update item returned by `getUpdates`.
 */
@Serializable
internal data class TelegramUpdate(
    val update_id: Long,
    val message: TelegramMessage? = null,
    val edited_message: TelegramMessage? = null,
    val channel_post: TelegramMessage? = null,
)
