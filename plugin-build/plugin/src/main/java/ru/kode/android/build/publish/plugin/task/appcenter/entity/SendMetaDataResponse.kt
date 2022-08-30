package ru.kode.android.build.publish.plugin.task.appcenter.entity

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Suppress("ConstructorParameterNaming") // network model
internal data class SendMetaDataResponse(
    val error: Boolean?,
    val id: String?,
    val chunk_size: Long,
    val resume_restart: Boolean?,
    val chunk_list: List<Int>,
    val blob_partitions: Int,
    val status_code: String,
)
