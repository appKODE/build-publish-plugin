package ru.kode.android.build.publish.plugin.firebase.controller.entity

import kotlinx.serialization.Serializable

/**
 * One of the value kinds a parameter can hold. Exactly one field is set by the API.
 */
@Serializable
data class RemoteConfigParameterValue(
    val value: String? = null,
    val useInAppDefault: Boolean? = null,
    val rolloutValue: Rollout? = null,
    val personalizationValue: Personalization? = null,
) {
    /**
     * Value served by a Firebase Rollout to [percent] of the users.
     */
    @Serializable
    data class Rollout(
        val rolloutId: String,
        val value: String? = null,
        val percent: Int? = null,
    )

    /**
     * Value chosen by Remote Config Personalization.
     */
    @Serializable
    data class Personalization(
        val personalizationId: String,
    )

    /**
     * Short human readable form used in reports.
     */
    fun display(): String =
        when {
            value != null -> value
            useInAppDefault == true -> "<in-app default>"
            rolloutValue != null ->
                "rollout ${rolloutValue.rolloutId}: ${rolloutValue.value} (${rolloutValue.percent}%)"
            personalizationValue != null -> "personalization ${personalizationValue.personalizationId}"
            else -> "<empty>"
        }
}
