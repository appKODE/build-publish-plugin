package ru.kode.android.build.publish.plugin.telegram.network.entity

import kotlinx.serialization.Serializable

/**
 * Response payload for Telegram Bot API `getUpdates`.
 */
@Serializable
internal data class TelegramUpdateResponse(
    val ok: Boolean,
    val result: List<TelegramUpdate>,
)
