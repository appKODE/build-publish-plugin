package ru.kode.android.build.publish.plugin.telegram.controller.entity

/**
 * Represents the last message received from a Telegram chat.
 *
 * @param text The text of the last message.
 */
@kotlinx.serialization.Serializable
data class TelegramLastMessage(
    val text: String?,
    val chatName: String,
    val chatId: String,
    val topicName: String?,
    val topicId: String?
)
