package ru.kode.android.build.publish.plugin.clickup.network.entity

import kotlinx.serialization.Serializable

/**
 * ClickUp API response for listing teams/workspaces available for the current token.
 */
@Serializable
internal data class GetTeamsResponse(
    val teams: List<Team>,
) {
    @Serializable
    internal data class Team(
        val id: String,
        val name: String,
    )
}
