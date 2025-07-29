package ru.kode.android.build.publish.plugin.core.enity
import org.gradle.api.provider.Provider

data class TaskOutput(
    val versionCode: Provider<Int>? = null,
    val versionName: Provider<String>? = null,
    val apkOutputFileName: String
)
