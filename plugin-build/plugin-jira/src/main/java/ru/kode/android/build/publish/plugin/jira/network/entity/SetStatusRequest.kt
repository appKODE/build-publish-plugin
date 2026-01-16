package ru.kode.android.build.publish.plugin.jira.network.entity

import kotlinx.serialization.Serializable

/**
 * Jira API request body for transitioning an issue to a different status.
 */
@Serializable
internal data class SetStatusRequest(
    val transition: Transition,
) {
    @Serializable
    data class Transition(
        val id: String,
    )
}
