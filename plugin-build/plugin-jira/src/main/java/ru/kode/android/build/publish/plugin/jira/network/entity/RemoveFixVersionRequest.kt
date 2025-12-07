package ru.kode.android.build.publish.plugin.jira.network.entity

import kotlinx.serialization.Serializable

@Serializable
internal data class RemoveFixVersionRequest(
    val update: Update,
) {
    @Serializable
    data class Update(
        val fixVersions: List<FixVersionRemove>,
    )

    @Serializable
    data class FixVersionRemove(
        val remove: VersionName,
    )

    @Serializable
    data class VersionName(
        val name: String,
    )
}