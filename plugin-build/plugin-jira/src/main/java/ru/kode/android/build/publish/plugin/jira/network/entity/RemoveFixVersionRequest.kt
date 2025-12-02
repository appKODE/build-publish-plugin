package ru.kode.android.build.publish.plugin.jira.network.entity

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Suppress("ConstructorParameterNaming") // network model
internal data class RemoveFixVersionRequest(
    val update: Update,
) {
    @JsonClass(generateAdapter = true)
    @Suppress("ConstructorParameterNaming") // network model
    data class Update(
        val fixVersions: List<FixVersionRemove>,
    )

    @JsonClass(generateAdapter = true)
    @Suppress("ConstructorParameterNaming") // network model
    data class FixVersionRemove(
        val remove: VersionName,
    )

    @JsonClass(generateAdapter = true)
    @Suppress("ConstructorParameterNaming") // network model
    data class VersionName(
        val name: String,
    )
}