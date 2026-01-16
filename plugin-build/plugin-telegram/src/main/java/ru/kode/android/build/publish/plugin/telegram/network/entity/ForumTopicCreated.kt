package ru.kode.android.build.publish.plugin.telegram.network.entity

import kotlinx.serialization.Serializable

/**
 * Forum topic creation payload included in some Telegram messages.
 */
@Serializable
internal data class ForumTopicCreated(
    val name: String,
)
