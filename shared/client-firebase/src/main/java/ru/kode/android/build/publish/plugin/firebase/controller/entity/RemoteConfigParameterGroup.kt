package ru.kode.android.build.publish.plugin.firebase.controller.entity

import kotlinx.serialization.Serializable

/**
 * Group of parameters shown together in the Firebase console.
 */
@Serializable
data class RemoteConfigParameterGroup(
    val description: String? = null,
    val parameters: Map<String, RemoteConfigParameter> = emptyMap(),
)
