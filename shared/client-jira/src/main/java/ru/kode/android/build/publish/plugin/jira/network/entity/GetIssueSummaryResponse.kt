package ru.kode.android.build.publish.plugin.jira.network.entity

import kotlinx.serialization.Serializable

/**
 * Jira API response for retrieving an issue's summary (title).
 */
@Serializable
internal data class GetIssueSummaryResponse(
    val fields: Fields,
) {
    @Serializable
    internal data class Fields(
        val summary: String,
    )
}
