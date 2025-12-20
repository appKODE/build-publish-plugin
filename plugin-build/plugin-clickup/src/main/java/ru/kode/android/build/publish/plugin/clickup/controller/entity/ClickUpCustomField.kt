package ru.kode.android.build.publish.plugin.clickup.controller.entity

data class ClickUpCustomField(
    val id: String,
    val name: String,
    val type: String,
    val value: String?,
)
