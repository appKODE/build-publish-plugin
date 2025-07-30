package ru.kode.android.build.publish.plugin.jira.task.entity

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Suppress("ConstructorParameterNaming") // network model
data class AddFixVersionRequest(
    val update: Update,
) {
    @JsonClass(generateAdapter = true)
    @Suppress("ConstructorParameterNaming") // network model
    data class Update(
        val fixVersions: List<FixVersion>,
    )

    @JsonClass(generateAdapter = true)
    @Suppress("ConstructorParameterNaming") // network model
    data class FixVersion(
        val add: Description,
    ) {
        @JsonClass(generateAdapter = true)
        @Suppress("ConstructorParameterNaming") // network model
        data class Description(
            val name: String,
        )
    }
}
