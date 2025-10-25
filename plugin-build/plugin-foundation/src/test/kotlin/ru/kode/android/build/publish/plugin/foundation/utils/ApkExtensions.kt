package ru.kode.android.build.publish.plugin.foundation.utils

import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal val currentDate
    get() = LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyyyy"))

private val sdkPath
    get() = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")

private val apkanalyzerPath
    get() = File(sdkPath, "cmdline-tools/latest/bin/apkanalyzer").absolutePath

internal fun File.extractManifestProperties(): ManifestProperties {
    val manifestOutput =
        ProcessBuilder()
            .command(apkanalyzerPath, "manifest", "print", this.absolutePath)
            .start()
            .inputStream
            .bufferedReader()
            .readText()

    println("--- MANIFEST START ---")
    println(manifestOutput)
    println("--- MANIFEST END ---")

    val versionCodeMatch = Regex("versionCode=\"(\\d+)\"").find(manifestOutput)
    val versionNameMatch = Regex("versionName=\"([^\"]+)\"").find(manifestOutput)
    val versionCode = versionCodeMatch?.groupValues?.get(1).orEmpty()
    val versionName = versionNameMatch?.groupValues?.get(1).orEmpty()

    return ManifestProperties(
        versionCode = versionCode,
        versionName = versionName,
    )
}

internal data class ManifestProperties(
    val versionCode: String,
    val versionName: String,
)
