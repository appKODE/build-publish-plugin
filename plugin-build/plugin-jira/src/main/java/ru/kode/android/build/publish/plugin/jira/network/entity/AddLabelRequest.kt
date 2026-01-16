package ru.kode.android.build.publish.plugin.jira.network.entity

import kotlinx.serialization.Serializable

/**
 * Jira API request body for adding a label to an issue.
 */
@Serializable
internal data class AddLabelRequest(
    val update: Update,
) {
    @Serializable
    data class Update(
        val labels: List<Label>,
    )

    @Serializable
    data class Label(
        val add: String,
    )
}
