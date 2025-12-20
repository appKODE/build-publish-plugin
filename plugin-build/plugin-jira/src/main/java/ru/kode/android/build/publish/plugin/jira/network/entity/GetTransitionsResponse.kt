package ru.kode.android.build.publish.plugin.jira.network.entity

import kotlinx.serialization.Serializable

@Serializable
internal data class GetTransitionsResponse(
    val transitions: List<JiraTransition>,
) {
    @Serializable
    internal data class JiraTransition(
        val id: String,
        val name: String,
        val to: JiraStatus,
    )

    @Serializable
    internal data class JiraStatus(
        val id: String,
        val name: String,
    )
}
