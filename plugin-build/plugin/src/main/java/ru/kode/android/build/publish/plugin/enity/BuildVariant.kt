package ru.kode.android.build.publish.plugin.enity

import java.io.File

data class BuildVariant(
    val name: String,
    val outputFile: File
)
