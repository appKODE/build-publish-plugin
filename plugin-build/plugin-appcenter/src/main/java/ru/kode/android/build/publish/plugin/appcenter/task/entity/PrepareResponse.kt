package ru.kode.android.build.publish.plugin.appcenter.task.entity

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Suppress("ConstructorParameterNaming") // network model
internal data class PrepareResponse(
    val id: String,
    val package_asset_id: String,
    val upload_domain: String?,
    val token: String,
    val url_encoded_token: String,
)
