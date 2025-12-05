package ru.kode.android.build.publish.plugin.telegram.network.entity

import kotlinx.serialization.Serializable

@Serializable
internal data class TelegramChat(
    val id: Long
)