package ru.kode.android.build.publish.plugin.yandexdisk.network.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class YandexDiskLink(
    val href: String,
    val method: String? = null,
    val templated: Boolean? = null,
)

@Serializable
internal data class YandexDiskResource(
    @SerialName("public_url")
    val publicUrl: String? = null,
)
