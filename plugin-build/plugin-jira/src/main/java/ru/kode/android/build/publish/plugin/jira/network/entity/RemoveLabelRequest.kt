package ru.kode.android.build.publish.plugin.jira.network.entity

import kotlinx.serialization.Serializable

@Serializable
internal data class RemoveLabelRequest(
    val update: Update,
) {
    @Serializable
    data class Update(
        val labels: List<LabelRemove>,
    )

    @Serializable
    data class LabelRemove(
        val remove: String,
    )
}