package ru.kode.android.build.publish.plugin.firebase.network.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Fields of a Google service account JSON key needed to mint an OAuth access token.
 */
@Serializable
internal data class ServiceAccountKey(
    @SerialName("client_email")
    val clientEmail: String,
    @SerialName("private_key")
    val privateKey: String,
    @SerialName("token_uri")
    val tokenUri: String = "https://oauth2.googleapis.com/token",
)
