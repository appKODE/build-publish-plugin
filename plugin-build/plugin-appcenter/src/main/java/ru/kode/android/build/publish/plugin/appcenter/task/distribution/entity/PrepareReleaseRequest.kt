package ru.kode.android.build.publish.plugin.appcenter.task.distribution.entity

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Suppress("ConstructorParameterNaming") // network model
data class PrepareReleaseRequest(
    val build_version: String,
    val build_number: String,
)
