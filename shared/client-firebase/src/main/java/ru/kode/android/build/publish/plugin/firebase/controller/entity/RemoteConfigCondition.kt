package ru.kode.android.build.publish.plugin.firebase.controller.entity

import kotlinx.serialization.Serializable

/**
 * Named condition of a Remote Config template, e.g. `app.version >= '1.2.0' && device.os == 'android'`.
 */
@Serializable
data class RemoteConfigCondition(
    val name: String,
    val expression: String,
)
