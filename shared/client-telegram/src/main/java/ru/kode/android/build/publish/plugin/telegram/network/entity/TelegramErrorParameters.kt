package ru.kode.android.build.publish.plugin.telegram.network.entity

import kotlinx.serialization.Serializable

/**
 * Optional error parameters returned by Telegram Bot API.
 */
@Serializable
internal data class TelegramErrorParameters(
    val retry_after: Long? = null,
)
