package ru.kode.android.build.publish.plugin.task.confluence.entity

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AddCommentRequest(
    val type: String = "comment",
    val container: Container,
    val body: Body,
)

@JsonClass(generateAdapter = true)
data class Container(
    val id: String,
    val type: String = "page",
)

@JsonClass(generateAdapter = true)
data class Body(
    val storage: Storage,
)

@JsonClass(generateAdapter = true)
data class Storage(
    val value: String,
    val representation: String = "storage",
)
