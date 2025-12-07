package ru.kode.android.build.publish.plugin.jira.network.entity

import kotlinx.serialization.Serializable

@Serializable
internal data class AddFixVersionRequest(
    val update: Update,
) {
    @Serializable
    data class Update(
        val fixVersions: List<FixVersion>,
    )

    @Serializable
    data class FixVersion(
        val add: Description,
    ) {
        @Serializable
        data class Description(
            val name: String,
        )
    }
}
