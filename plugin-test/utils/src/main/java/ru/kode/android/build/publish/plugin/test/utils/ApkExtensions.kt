package ru.kode.android.build.publish.plugin.test.utils

import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

val currentDate: String
    get() = LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyyyy"))

private val sdkPath
    get() = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")

private val apkAnalyzerPath: String
    get() {
        val sdk = sdkPath ?: error("ANDROID_HOME or ANDROID_SDK_ROOT not set")
        val buildToolsDir = File(sdk, "build-tools")
        val buildToolsVersion = buildToolsDir.listFiles()?.maxByOrNull { it.name }
        val fileName = if (isWindows()) "apkanalyzer.bat" else "apkanalyzer"
        val candidate = buildToolsVersion?.resolve(fileName)
        return if (candidate?.exists() == true) {
            candidate.absolutePath
        } else {
            val filePath = "cmdline-tools/latest/bin/$fileName"
            File(sdk, filePath).absolutePath
        }
    }

private fun isWindows(): Boolean {
    return System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
}

fun File.extractManifestProperties(): ManifestProperties {
    val manifestOutput =
        ProcessBuilder()
            .command(apkAnalyzerPath, "manifest", "print", this.absolutePath)
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

data class ManifestProperties(
    val versionCode: String,
    val versionName: String,
)
