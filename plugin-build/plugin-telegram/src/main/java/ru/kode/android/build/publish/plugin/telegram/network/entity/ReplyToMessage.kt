package ru.kode.android.build.publish.plugin.telegram.network.entity

import kotlinx.serialization.Serializable

@Serializable
internal data class ReplyToMessage(
    val forum_topic_created: ForumTopicCreated? = null,
)
