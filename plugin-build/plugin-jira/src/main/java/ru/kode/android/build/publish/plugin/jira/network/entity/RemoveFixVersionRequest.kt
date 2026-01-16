package ru.kode.android.build.publish.plugin.jira.network.entity

import kotlinx.serialization.Serializable

/**
 * Jira API request body for removing a fix version from an issue.
 */
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
