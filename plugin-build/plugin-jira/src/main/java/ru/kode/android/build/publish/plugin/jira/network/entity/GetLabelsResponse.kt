package ru.kode.android.build.publish.plugin.jira.network.entity

import kotlinx.serialization.Serializable

@Serializable
internal data class GetLabelsResponse(
    val fields: Fields,
) {
    @Serializable
    data class Fields(
        val labels: List<String>,
    )
}