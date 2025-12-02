package ru.kode.android.build.publish.plugin.jira.network.entity

import com.squareup.moshi.JsonClass
import org.gradle.internal.impldep.kotlinx.serialization.Serializable

@JsonClass(generateAdapter = true)
@Suppress("ConstructorParameterNaming") // network model
internal data class GetLabelsResponse(
    val fields: Fields,
) {
    @JsonClass(generateAdapter = true)
    @Suppress("ConstructorParameterNaming") // network model
    data class Fields(
        val labels: List<String>,
    )
}