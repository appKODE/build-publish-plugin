package ru.kode.android.build.publish.plugin.task.jira.entity

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Suppress("ConstructorParameterNaming") // network model
data class SetStatusRequest(
    val transition: Transition
) {
    @JsonClass(generateAdapter = true)
    @Suppress("ConstructorParameterNaming") // network model
    data class Transition(
        val id: String,
    )
}
