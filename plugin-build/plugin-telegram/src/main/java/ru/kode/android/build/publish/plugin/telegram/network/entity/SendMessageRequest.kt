package ru.kode.android.build.publish.plugin.telegram.network.entity

import kotlinx.serialization.Serializable

/**
 * Request payload for Telegram Bot API `sendMessage`.
 */
@Serializable
data class SendMessageRequest(
    val chat_id: String,
    val message_thread_id: String? = null,
    val text: String,
    val parse_mode: String,
    val disable_web_page_preview: Boolean,
)
