package ru.kode.android.build.publish.plugin.firebase.controller.entity

import kotlinx.serialization.Serializable

/**
 * Remote Config parameter: the default value plus values keyed by condition name.
 */
@Serializable
data class RemoteConfigParameter(
    val defaultValue: RemoteConfigParameterValue? = null,
    val conditionalValues: Map<String, RemoteConfigParameterValue> = emptyMap(),
    val description: String? = null,
    val valueType: String? = null,
)
