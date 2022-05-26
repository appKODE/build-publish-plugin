package ru.kode.android.build.publish.plugin.enity

internal data class BuildVariant(
    val flavorNames: List<String>, // must be ordered by flavor dimension priority
    val buildTypeName: String
)
