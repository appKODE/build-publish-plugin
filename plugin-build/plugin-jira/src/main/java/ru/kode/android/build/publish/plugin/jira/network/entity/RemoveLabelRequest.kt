package ru.kode.android.build.publish.plugin.jira.network.entity

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Suppress("ConstructorParameterNaming") // network model
internal data class RemoveLabelRequest(
    val update: Update,
) {
    @JsonClass(generateAdapter = true)
    @Suppress("ConstructorParameterNaming") // network model
    data class Update(
        val labels: List<LabelRemove>,
    )

    @JsonClass(generateAdapter = true)
    @Suppress("ConstructorParameterNaming") // network model
    data class LabelRemove(
        val remove: String,
    )
}