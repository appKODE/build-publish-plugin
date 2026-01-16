package ru.kode.android.build.publish.plugin.telegram.network.entity

import kotlinx.serialization.Serializable

/**
 * Reply-to message wrapper used in Telegram message payloads.
 */
@Serializable
internal data class ReplyToMessage(
    val forum_topic_created: ForumTopicCreated? = null,
)
