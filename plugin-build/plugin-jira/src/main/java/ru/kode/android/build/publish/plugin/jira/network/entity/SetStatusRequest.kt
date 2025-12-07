package ru.kode.android.build.publish.plugin.jira.network.entity

import kotlinx.serialization.Serializable

@Serializable
internal data class SetStatusRequest(
    val transition: Transition,
) {
    @Serializable
    data class Transition(
        val id: String,
    )
}