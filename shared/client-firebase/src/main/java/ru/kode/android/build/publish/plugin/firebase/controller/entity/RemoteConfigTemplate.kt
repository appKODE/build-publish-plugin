package ru.kode.android.build.publish.plugin.firebase.controller.entity

import kotlinx.serialization.Serializable

/**
 * Firebase Remote Config template as returned by the REST API
 * (`GET v1/projects/{project}/remoteConfig`).
 *
 * Only the fields needed for auditing are modelled; unknown fields are ignored.
 */
@Serializable
data class RemoteConfigTemplate(
    val conditions: List<RemoteConfigCondition> = emptyList(),
    val parameters: Map<String, RemoteConfigParameter> = emptyMap(),
    val parameterGroups: Map<String, RemoteConfigParameterGroup> = emptyMap(),
    val version: RemoteConfigVersion? = null,
) {
    /**
     * Top-level parameters together with the parameters nested in [parameterGroups].
     */
    val allParameters: Map<String, RemoteConfigParameter>
        get() = parameters + parameterGroups.values.flatMap { it.parameters.entries }.associate { it.key to it.value }
}
